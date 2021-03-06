package org.exoplatform.mfa.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.exoplatform.commons.api.settings.ExoFeatureService;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.mfa.storage.MfaStorage;
import org.exoplatform.mfa.storage.dto.RevocationRequest;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;

public class MfaService {

  public static final String                    MFA_FEATURE               = "mfa";

  private static final String                   MFA_SYSTEM_SETTING        = "mfaSystem";

  private static final String                   MFA_PROTECTED_GROUPS      = "protectedGroups";

  private static final String                   MFA_PROTECTED_NAVIGATIONS = "protectedGroupNavigations";

  private String                                mfaSystem;

  private ExoFeatureService                     featureService;

  private SettingService                        settingService;

  private List<String>                          protectedNavigations      = new ArrayList<>();

  private List<String>                          protectedGroups;

  private MfaStorage                            mfaStorage;

  private ListenerService                       listenerService;

  private static final Log                      LOG                       = ExoLogger.getLogger(MfaService.class);

  private Map<String, MfaSystemComponentPlugin> mfaSystemServices;

  public MfaService(InitParams initParams,
                    MfaStorage mfaStorage,
                    ExoFeatureService featureService,
                    SettingService settingService,
                    ListenerService listenerService) {
    this.featureService = featureService;
    this.settingService = settingService;
    this.listenerService = listenerService;

    String protectedGroupNavigationsValue = "";
    if (settingService.get(Context.GLOBAL, Scope.GLOBAL, MFA_PROTECTED_NAVIGATIONS) != null) {
      protectedGroupNavigationsValue = settingService.get(Context.GLOBAL, Scope.GLOBAL, MFA_PROTECTED_NAVIGATIONS)
                                                     .getValue()
                                                     .toString();
    } else {
      protectedGroupNavigationsValue = initParams.getValueParam(MFA_PROTECTED_NAVIGATIONS).getValue();
    }
    if (protectedGroupNavigationsValue.isEmpty()) {
      this.protectedNavigations = new ArrayList<>();
    } else {
      this.protectedNavigations = Arrays.asList(protectedGroupNavigationsValue.split(","));
    }

    String protectedGroupsValue = "";
    if (settingService.get(Context.GLOBAL, Scope.GLOBAL, MFA_PROTECTED_GROUPS) != null) {
      protectedGroupsValue = settingService.get(Context.GLOBAL, Scope.GLOBAL, MFA_PROTECTED_GROUPS).getValue().toString();
    } else {
      protectedGroupsValue = initParams.getValueParam(MFA_PROTECTED_GROUPS).getValue();
    }
    this.protectedGroups = Arrays.stream(protectedGroupsValue.split(",")).filter(s -> !s.isEmpty()).collect(Collectors.toList());

    mfaSystemServices = new HashMap<>();
    if (settingService.get(Context.GLOBAL, Scope.GLOBAL, MFA_SYSTEM_SETTING) != null
        && !settingService.get(Context.GLOBAL, Scope.GLOBAL, MFA_SYSTEM_SETTING).getValue().toString().isEmpty()) {
      this.mfaSystem = settingService.get(Context.GLOBAL, Scope.GLOBAL, MFA_SYSTEM_SETTING).getValue().toString();
    } else {
      this.mfaSystem = initParams.getValueParam(MFA_SYSTEM_SETTING).getValue();
    }
    this.mfaStorage = mfaStorage;
    mfaSystemServices = new HashMap<>();
  }

  public MfaSystemService getMfaSystemService(String type) {
    return mfaSystemServices.get(type).getMfaSystemService();
  }

  public void addConnector(MfaSystemComponentPlugin mfaSystemComponentPlugin) {
    this.mfaSystemServices.put(mfaSystemComponentPlugin.getMfaSystemService().getType(), mfaSystemComponentPlugin);
  }

  public boolean isProtectedUri(String requestUri) {
    return protectedNavigations.stream().anyMatch(requestUri::contains);

  }

  public boolean currentUserIsInProtectedGroup(Identity identity) {
    return protectedGroups.stream().anyMatch(identity::isMemberOf);
  }

  public String getMfaSystem() {
    return this.mfaSystem;
  }

  public MfaSystemService getMfaSystemService() {
    return this.mfaSystemServices.get(mfaSystem).getMfaSystemService();
  }

  public boolean addRevocationRequest(String username, String mfaType) {
    if (!hasRevocationRequest(username, mfaType)) {
      RevocationRequest revocationRequest = new RevocationRequest();
      revocationRequest.setUser(username);
      revocationRequest.setType(mfaType);
      mfaStorage.createRevocationRequest(revocationRequest);
      try {
        listenerService.broadcast(new Event("mfa.listener.create.revocation.request", null, username));
      } catch (Exception e) {
        LOG.error("Error when broadcasting mfa revocation request event", e);
      }
      return true;
    } else {
      return false;
    }
  }

  public boolean hasRevocationRequest(String username, String mfaType) {
    return mfaStorage.countByUsernameAndType(username, mfaType) > 0;
  }

  public void deleteRevocationRequest(String username, String type) {
    mfaStorage.deleteRevocationRequest(username, type);
  }

  public List<RevocationRequest> getAllRevocationRequests() {
    return mfaStorage.findAll();
  }

  public RevocationRequest getRevocationRequestById(Long id) {
    return mfaStorage.findById(id);
  }

  public void confirmRevocationRequest(Long id) {
    RevocationRequest revocationRequest = mfaStorage.findById(id);
    String type = revocationRequest.getType();
    mfaSystemServices.get(type).getMfaSystemService().removeSecret(revocationRequest.getUser());
    mfaStorage.deleteById(id);
  }

  public void cancelRevocationRequest(Long id) {
    mfaStorage.deleteById(id);
  }

  public List<String> getAvailableMfaSystems() {
    return new ArrayList<>(this.mfaSystemServices.keySet());
  }

  public void saveActiveFeature(String status) {
    featureService.saveActiveFeature(MFA_FEATURE, Boolean.parseBoolean(status));
  }

  public boolean setMfaSystem(String mfaSystem) {
    if (mfaSystemServices.containsKey(mfaSystem)) {
      settingService.set(Context.GLOBAL, Scope.GLOBAL, MFA_SYSTEM_SETTING, new SettingValue<>(mfaSystem));
      this.mfaSystem = mfaSystem;
      return true;
    } else {
      return false;
    }
  }

  public boolean isMfaFeatureActivated() {

    return featureService.isActiveFeature(MFA_FEATURE);
  }

  public void saveProtectedGroups(String groups) {
    settingService.set(Context.GLOBAL, Scope.GLOBAL, MFA_PROTECTED_GROUPS, new SettingValue<>(groups));
    if (groups.isEmpty()) {
      this.protectedGroups = new ArrayList<>();
    } else {
      this.protectedGroups = Arrays.asList(groups.split(","));
    }
  }

  public List<String> getProtectedGroups() {
    return this.protectedGroups;
  }

  public void saveProtectedNavigations(String navigations) {
    settingService.set(Context.GLOBAL, Scope.GLOBAL, MFA_PROTECTED_NAVIGATIONS, new SettingValue<>(navigations));
    if (navigations.isEmpty()) {
      this.protectedNavigations = new ArrayList<>();
    } else {
      this.protectedNavigations = Arrays.asList(navigations.split(","));
    }
  }

  public void deleteProtectedNavigations(String navigation) {
    this.protectedNavigations = this.protectedNavigations.stream()
                                                         .filter(nav -> !nav.equals(navigation))
                                                         .collect(Collectors.toList());
  }

  public List<MfaNavigations> getProtectedNavigations() {
    List<MfaNavigations> mfaNavigations = new LinkedList<>();
    for (int i = 0; i < this.protectedNavigations.size(); i++) {
      MfaNavigations mfaNavigations1 = new MfaNavigations();
      mfaNavigations1.setId(this.protectedNavigations.get(i));
      mfaNavigations.add(mfaNavigations1);
    }
    return mfaNavigations;
  }
}

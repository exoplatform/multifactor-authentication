package org.exoplatform.mfa.rest.mfa;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.mfa.api.MfaNavigations;
import org.exoplatform.mfa.api.MfaService;
import org.exoplatform.mfa.rest.entities.RevocationRequestEntity;
import org.exoplatform.mfa.storage.dto.RevocationRequest;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;


@Path("/mfa")
@Tag(name = "/mfa", description = "Manage MFA (multifactor-authentication) operations")
public class MfaRestService implements ResourceContainer {

  private MfaService       mfaService;

  private static final Log LOG = ExoLogger.getLogger(MfaRestService.class);

  public MfaRestService(MfaService mfaService) {
    this.mfaService = mfaService;
  }

  @Path("/settings")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Get Activated MFA System", method = "GET", description = "Get Activated MFA System")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error"), })
  public Response getMfaSystem(@Context HttpServletRequest request) {
    JSONObject result = new JSONObject();
    Locale locale = request == null ? Locale.ENGLISH : request.getLocale();
    try {
      result.put("mfaSystem", mfaService.getMfaSystemService().getType());
      result.put("helpTitle", mfaService.getMfaSystemService().getHelpTitle(locale));
      result.put("helpContent", mfaService.getMfaSystemService().getHelpContent(locale));
      return Response.ok().entity(result.toString()).build();
    } catch (JSONException e) {
      return Response.serverError().build();

    }
  }

  @Path("/available")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Get Activated MFA System", method = "GET", description = "Get Activated MFA System")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error"), })
  public Response getAvalailableMfaSystems() {
    JSONObject result = new JSONObject();
    try {
      result.put("available", mfaService.getAvailableMfaSystems());
      return Response.ok().entity(result.toString()).build();
    } catch (JSONException e) {
      return Response.serverError().build();

    }
  }

  @Path("/changeMfaFeatureActivation/{status}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @Operation(
          summary = "Switch the Activated MFA System",
          method = "PUT",
          description = "Switch the Activated MFA System")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error"), })
  public Response changeMfaFeatureActivation(@Parameter(description = "Switch the Activated MFA System to activated or deactivated", required = true) String status) {
    mfaService.saveActiveFeature(status);
    return Response.ok().type(MediaType.TEXT_PLAIN).build();
  }

  @Path("/revocations")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @Operation(summary = "Get Revocation Request list", method = "GET", description = "Get Revocation Request list")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getRevocationRequests() {

    List<RevocationRequest> revocationRequests = mfaService.getAllRevocationRequests();

    try {
      JSONObject result = new JSONObject();
      result.put("requests",
                 revocationRequests.stream()
                                   .map(this::buildRevocationRequest)
                                   .map(revocationRequestEntity -> new JSONObject(revocationRequestEntity.asMap()))
                                   .collect(Collectors.toList()));

      return Response.ok().entity(result.toString()).build();
    } catch (JSONException e) {
      LOG.error("Unable to build get revocation request answer", e);
      return Response.serverError().build();
    }
  }

  @Path("/revocations/{id}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @Operation(summary = "Update a revocation request", method = "PUT", description = "Update a revocation request")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response updateRevocationRequests(@Parameter(description = "RevocationRequest id", required = true) @PathParam("id") String id,
                                           @Parameter(description = "RevocationRequest status confirm/cancel", required = true) @QueryParam("status") String status) {

    RevocationRequest revocationRequest = mfaService.getRevocationRequestById(Long.parseLong(id));
    if (revocationRequest != null) {
      switch (status) {
      case "confirm":
        mfaService.confirmRevocationRequest(Long.parseLong(id));
        break;
      case "cancel":
        mfaService.cancelRevocationRequest(Long.parseLong(id));
        break;
      default:
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      return Response.ok().build();
    } else {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @Path("/revocations")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Create a Revocation Request", method = "POST", description = "Create a Revocation Request")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response addRevocationRequest(String type) {
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    boolean result = mfaService.addRevocationRequest(userId, type);
    return Response.ok().entity("{\"result\":\"" + result + "\"}").build();
  }

  private RevocationRequestEntity buildRevocationRequest(RevocationRequest revocationRequest) {
    return new RevocationRequestEntity(revocationRequest.getId(), revocationRequest.getUser(), revocationRequest.getType());
  }

  @Path("/changeMfaSystem/{mfaSystem}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @Operation(summary = "Change the MFA System", method = "PUT")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error"), })
  public Response changeMfaSystem(@Parameter(description = "Change the MFA MFA System", required = true) String mfaSystem) {
    if (mfaService.setMfaSystem(mfaSystem)) {
      return Response.ok().type(MediaType.TEXT_PLAIN).build();
    } else {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @POST
  @Path("/saveProtectedGroups")
  @RolesAllowed("administrators")
  @Operation(summary = "set mfa groups", method = "POST", description = "set mfa groups")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response saveProtectedGroups(@Parameter(description = "groups", required = true) String groups) {
    mfaService.saveProtectedGroups(groups);
    return Response.ok().entity("{\"groups\":\"" + groups + "\"}").build();
  }

  @Path("/getProtectedGroups")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @Operation(summary = "Get protected groups for MFA System", method = "GET", description = "Get protected groups for MFA System")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error"), })
  public Response getProtectedGroups() {

    JSONArray groups = new JSONArray();
    mfaService.getProtectedGroups().stream().forEach(groups::put);
    return Response.ok().entity("{\"protectedGroups\":" + groups + "}").build();
  }

  @Path("/getProtectedNavigations")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @Operation(summary = "Get protected navigations for MFA System", method = "GET", description = "Get protected navigations for MFA System")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error"), })
  public Response getProtectedNavigations() {

    List<MfaNavigations> mfaNavigations = mfaService.getProtectedNavigations();
    return Response.ok().entity(mfaNavigations).build();
  }

  @POST
  @Path("/saveProtectedNavigations")
  @RolesAllowed("administrators")
  @Operation(summary = "set mfa navigations", method = "POST", description = "set mfa navigations")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response saveProtectedNavigations(@Parameter(description = "navigations", required = true) String navigations) {
    mfaService.saveProtectedNavigations(navigations);
    return Response.ok().entity("{\"navigations\":\"" + navigations + "\"}").build();
  }

  @DELETE
  @Path("/deleteNavigation")
  @RolesAllowed("administrators")
  @Operation(summary = "Delete a navigation", method = "DELETE", description = "This delete the protected navigation")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response deleteNavigation(@Parameter(description = "navigation", required = true) String navigation) {
    mfaService.deleteProtectedNavigations(navigation);
    return Response.ok().build();
  }
}

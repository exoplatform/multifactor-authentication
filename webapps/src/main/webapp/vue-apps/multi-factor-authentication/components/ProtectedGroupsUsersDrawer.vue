<template>
  <exo-drawer
    id="protectedGroupsUsersDrawer"
    ref="protectedGroupsUsersDrawer"
    right
    @closed="drawer = false">
    <template slot="title">
      {{ $t('authentication.multifactor.protected.groups.users.title') }}
    </template>
    <template slot="content">
      <v-flex xs12 class="pa-3">
        <exo-identity-suggester
          v-model="groups"
          :ignore-items="ignoreItems"
          :labels="labels"
          :group-type="groupType"
          all-groups-for-admin
          multiple
          include-groups
          chips
          danse
          flat
          required />
      </v-flex>
    </template>
    <template slot="footer">
      <div class="d-flex ">
        <v-spacer />
        <v-btn
          class="btn me-2"
          @click="cancel">
          {{ $t('authentication.multifactor.protected.resources.button.cancel') }}
        </v-btn>
        <v-btn
          class="btn btn-primary"
          @click="save">
          {{ $t('authentication.multifactor.protected.resources.button.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>

<script>
import {getGroups, getProtectedGroups, saveProtectedGroups} from '../multiFactorServices';
export default {
  data () {
    return {
      drawer: false,
      groups: [],
      selectedGroups: [],
      ignoreItems: [],
      searchLoading: false,
      groupType: 'GROUP',
      labels: { 
        label: '',
        placeholder: this.$t('authentication.multifactor.protected.groups.users.placeholder'),  
        searchPlaceholder: '',
        noDataLabel: this.$t('authentication.multifactor.protected.noData')
      }
    };
  },
  watch: {
    drawer() {
      if (this.drawer) {
        this.$refs.protectedGroupsUsersDrawer.open();
      } else {
        this.$refs.protectedGroupsUsersDrawer.close();
      }
    },
  },
  created() {
    this.$root.$on('protectedGroupsUsers', this.protectedGroupsUsers);
    this.ignoreItems = this.groups.map(group =>({id: `${group.remoteId}`}));
  },
  methods: {
    protectedGroupsUsers(selectedGroups) {
      this.groups = [];
      this.selectedGroups = selectedGroups;
      this.selectedGroups.forEach(remoteId => {
        if (remoteId) {
          getGroups(remoteId).then(data => {
            for (const group of data.entities) {
              if (remoteId === group.groupName || remoteId === group.remoteId){
                this.groups.push({
                  displayName: group.label,
                  id: `group:${group.groupName}`,
                  profile: {
                    avatarUrl: null,
                    fullName: group.label,
                    id: group.groupName
                  },
                  providerId: 'group',
                  remoteId: group.groupName,
                  spaceId: null,                  
                });
              }
            }
          });
        }});
      this.drawer = true;
    },
    cancel() {
      this.drawer = false;
    },
    save() {
      const groups=[];
      this.groups.forEach(group => {
        if (group.remoteId){
          groups.push(group.remoteId);
        } else {
          groups.push(group.profile.id);
        }
      });
      saveProtectedGroups(groups.join(','));
      this.$root.$emit('protectedGroupsList', groups);
      this.$refs.protectedGroupsUsersDrawer.close();
    },
    getProtectedGroups() {
      getProtectedGroups().then(data => {
        for (const group of data.protectedGroups) {
          this.groups.push(group);
        }
      });
      const groups= [];
      this.selectedGroups.forEach(data => {
        groups.push(getGroups(data).then(data => {
          for (const group of data.entities) {
            groups.push({profile: {
              avatarUrl: null,
              fullName: group.label,
              id: group.id,
              type: 'group'}
            });
          }
        }));
      });
    },
  },
};
</script>

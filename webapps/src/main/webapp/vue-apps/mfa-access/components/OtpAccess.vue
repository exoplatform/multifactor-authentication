<template>
  <v-app>
    <v-container class="pa-0">
      <div
        align="center"
        justify="center">
        <div v-if="screen === 'askToken' || screen === 'registration'">
          <h class="font-weight-bold titleClass pb-3">{{ $t('mfa.otp.access.title') }}</h>
          <div class="otpAccessBlock lockIcon mt-5">
          </div>
        </div>
        <div v-if="screen === 'registration'">
          <div class="messageClass">1. {{ $t('mfa.otp.registration.step1') }}</div>
          <div class="messageClass">2. {{ $t('mfa.otp.registration.step2') }}</div>
        </div>
        <div id="qrCode"></div>
        <div v-if="screen === 'registration'">
          <div class="messageClass">
            {{ $t('mfa.otp.registration.alternativeStep2') }}<br>
            {{ secret }}
          </div>
          <div class="messageClass">3. {{ $t('mfa.otp.registration.step3') }}</div>
        </div>
        <div v-if="screen === 'askToken'">
          <div class="messageClass">{{ $t('mfa.otp.registration.step3') }}</div>
        </div>
        <div v-if="screen === 'askToken' || screen === 'registration'">
          <form
            id="otpForm"
            @submit.prevent="onSubmit">
            <input
              id="tokenInput"
              v-model="token"
              :placeholder="$t('mfa.otp.access.token.placeholder')"
              class="ignore-vuetify-classes"
              required
              type="text"
              name="token">
            <button class="btn btn-primary">
              {{ $t('mfa.otp.access.button.confirm') }}
            </button>
          </form>
          <div
            class="helpMessageClass mt-5 font-italic"
            v-if="screen === 'askToken'">
            {{ $t('mfa.otp.access.help.line1') }}<br>
            {{ $t('mfa.otp.access.help.line2') }}
            <a
              href="javascript:void(0)"
              class="font-weight-bold"
              @click="askRevocation">
              {{ $t('mfa.otp.access.help.link') }}
            </a>{{ $t('mfa.otp.access.help.line3') }}
          </div>
        </div>
        <div v-if="screen === 'error'">
          <div class="otpAccessBlock lockIcon">
            <i class="uiIconCloseCircled closeCircledIconColor"></i>
          </div>
          <div class="font-italic messageClass">{{ $t('mfa.otp.access.echec') }}</div>
        </div>
      </div>
    </v-container>
  </v-app>
</template>
<script>
export default {
  data() {
    return {
      token: '',
      screen: '',
      secret: '',
      secretSrc: '',
    };
  },
  mounted() {
    this.$nextTick().then(() => this.$root.$emit('application-loaded'));
  },
  created() {
    window.setTimeout(() => {
      this.checkRegistration();
    }, 1000);
  },
  methods: {
    getQueryParam(paramName) {
      const uri = window.location.search.substring(1);
      const params = new URLSearchParams(uri);
      return params.get(paramName);
    },
    changeScreen(screen) {
      this.screen = screen;
    },
    checkRegistration() {
      fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/otp/checkRegistration`, {
        method: 'GET',
        credentials: 'include',
      }).then(resp => resp && resp.ok && resp.json())
        .then(data => {
          if (data.result && data.result === 'true') {
            this.changeScreen('askToken');
            this.$nextTick().then(() => {
              document.getElementById('tokenInput').focus();
            });
          } else {
            this.startRegistration();
          }
        });
    },
    startRegistration() {
      fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/otp/generateSecret`, {
        method: 'GET',
        credentials: 'include',
      }).then(resp => resp && resp.ok && resp.json())
        .then(data => {
          if (data.secret) {
            window.require(['SHARED/qrcode'], () => {
              this.secret=data.secret;
              this.changeScreen('registration');
              new window.QRCode(document.getElementById('qrCode'), {text: data.url});
            });
          } else {
            this.changeScreen('error');
          }
        });
    },
    onSubmit() {
      fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/otp/verify?token=${this.token}`, {
        method: 'GET',
        credentials: 'include',
      }).then(resp => resp && resp.ok && resp.json())
        .then(data => {
          if (data.result) {
            window.location.href=this.getQueryParam('initialUri');
          }
        });
    },
    askRevocation() {
      fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/mfa/revocations`, {
        method: 'POST',
        credentials: 'include',
        body: 'OTP'
      }).then(resp => resp && resp.ok && resp.json())
        .then(data => {
          const message = data?.result  === 'true' && this.$t('mfa.otp.access.revocation.success') || this.$t('mfa.otp.access.revocation.warning');
          const type = data?.result  === 'true' && 'success' || 'warning';
          document.dispatchEvent(new CustomEvent('alert-message', {detail: {
              alertType: type,
              alertMessage: message,
            }}));
        })
    },
  },
};
</script>

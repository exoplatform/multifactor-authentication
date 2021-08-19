import { enableFetchMocks } from 'jest-fetch-mock'
enableFetchMocks();

import { shallowMount } from '@vue/test-utils'
import MfaAccess from '../webapp/vue-apps/mfa-access/components/MfaAccess.vue'
import OtpAccess from '../webapp/vue-apps/mfa-access/components/OtpAccess.vue'

describe("MfaAccess", () => {
  fetchMock.mockOnce("{\"mfaSystem\":\"OTP\"}");

  let extensionRegistry = {
    loadExtensions: jest.fn(() => [
      {
        id: 'OTP',
        title: 'otp',
        enabled: () => true,
        component: {
          name: 'mfa-otp-access',
          model: {
            value: [],
            default: []
          }
        }
      }
    ])
  }
  Object.defineProperty(global, 'extensionRegistry', {
      value: extensionRegistry
  })


  const wrapper = shallowMount(MfaAccess, {
    stubs: ['mfa-otp-access']
  });
  test("has data", () => {
    expect(wrapper.vm.extension.id).toBe('OTP')
  });


});

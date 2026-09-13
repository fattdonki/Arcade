// IFirewallService.aidl
package com.arthur.arcade;

interface IFirewallService {
    void setChain3Enabled(boolean enabled);
    void setPackageNetworkingEnabled(boolean enabled, String packageName);
}
package com.wbd101.hrvdemo;

import com.amazonaws.auth.AWSAbstractCognitoDeveloperIdentityProvider;
import com.amazonaws.regions.Regions;

public class DeveloperAuthenticationProvider extends AWSAbstractCognitoDeveloperIdentityProvider {

    private static final String developerProvider = "wbd101-hrvdemo";

    public DeveloperAuthenticationProvider(String accountId, String identityPoolId, Regions region) {
        super(accountId, identityPoolId, region);
        // Initialize any other objects needed here.
    }

    @Override
    public String getProviderName() {
        return developerProvider;
    }

    @Override
    public String refresh() {

        setToken(null);

        update(identityId, token);
        return token;

    }

    @Override
    public String getIdentityId() {

        if (identityId == null) {
            return getIdentityPoolId();
        } else {
            return identityId;
        }

    }
}


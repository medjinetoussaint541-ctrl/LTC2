package com.team.apk.Service;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

//Service personnalisé pour charger l'utilisateur OAuth2.
//, on s'appuie sur le service par défaut puis on ajuste l'attribut utilisé comme identifiant.
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    
//    Récupère les informations utilisateur depuis le fournisseur OAuth2.
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String defaultNameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        String nameAttributeKey = defaultNameAttributeKey;
        // Pour Google, on préfère utiliser l'email comme identifiant principal
        // lorsqu'il est disponible.
        if ("google".equalsIgnoreCase(registrationId) && oauth2User.getAttribute("email") != null) {
            nameAttributeKey = "email";
        }

        return new DefaultOAuth2User(
                AuthorityUtils.createAuthorityList("ROLE_USER"),
                oauth2User.getAttributes(),
                nameAttributeKey
        );
    }
}
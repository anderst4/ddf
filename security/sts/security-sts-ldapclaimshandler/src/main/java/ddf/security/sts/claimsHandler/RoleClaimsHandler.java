/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.sts.claimsHandler;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.sts.claims.Claim;
import org.apache.cxf.sts.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.RequestClaimCollection;
import org.apache.log4j.Logger;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class RoleClaimsHandler implements ClaimsHandler {



    private final Logger logger = Logger.getLogger(RoleClaimsHandler.class);

    private Map<String, String> claimsLdapAttributeMapping;

    private LdapTemplate ldapTemplate;

    private String delimiter = ";";

    private String objectClass = "groupOfNames";

    private String memberNameAttribute = "member";

    private String userNameAttribute = "uid";

    private String groupNameAttribute = "cn";

    private String userBaseDn;

    private String groupBaseDn;

    private String roleClaimType = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    private String propertyFileLocation;

    private String attributeMapping;

    public URI getRoleURI() {
        URI uri = null;
        try {
            uri = new URI(roleClaimType);
        } catch (URISyntaxException e) {
            logger.warn("Unable to add role claim type.", e);
        }
        return uri;
    }

    public String getPropertyFileLocation() {
        return propertyFileLocation;
    }

    public void setPropertyFileLocation(String propertyFileLocation) {
        if (propertyFileLocation != null
                && !propertyFileLocation.isEmpty() && !propertyFileLocation.equals(this.propertyFileLocation)) {
            setClaimsLdapAttributeMapping(AttributeMapLoader.buildClaimsMapFile(propertyFileLocation));
        }
        this.propertyFileLocation = propertyFileLocation;
    }

    public String getAttributeMapping() {
        return attributeMapping;
    }

    public void setAttributeMapping(String attributesToMap) {
        if (attributesToMap != null
                && !attributesToMap.isEmpty() && !attributesToMap.equals(this.attributeMapping)) {
            setClaimsLdapAttributeMapping(AttributeMapLoader.buildClaimsMap(attributesToMap));
        }
        this.attributeMapping = attributesToMap;
    }

    public String getRoleClaimType() {
        return roleClaimType;
    }

    public void setRoleClaimType(String roleClaimType) {
        this.roleClaimType = roleClaimType;
    }

    public String getGroupNameAttribute() {
        return groupNameAttribute;
    }

    public void setGroupNameAttribute(String groupNameAttribute) {
        this.groupNameAttribute = groupNameAttribute;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getGroupBaseDn() {
        return groupBaseDn;
    }

    public void setGroupBaseDn(String groupBaseDn) {
        this.groupBaseDn = groupBaseDn;
    }

    public LdapTemplate getLdapTemplate() {
        return ldapTemplate;
    }

    public void setLdapTemplate(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public String getMemberNameAttribute() {
        return memberNameAttribute;
    }

    public void setMemberNameAttribute(String memberNameAttribute) {
        this.memberNameAttribute = memberNameAttribute;
    }

    public String getUserBaseDn() {
        return userBaseDn;
    }

    public void setUserBaseDn(String userBaseDn) {
        this.userBaseDn = userBaseDn;
    }

    public void setClaimsLdapAttributeMapping(
            Map<String, String> ldapClaimMapping) {
        this.claimsLdapAttributeMapping = ldapClaimMapping;
    }

    public Map<String, String> getClaimsLdapAttributeMapping() {
        return claimsLdapAttributeMapping;
    }

    @Override
    public List<URI> getSupportedClaimTypes() {
        List<URI> uriList = new ArrayList<URI>();
        uriList.add(getRoleURI());

        return uriList;
    }

    @Override
    public ClaimCollection retrieveClaimValues(RequestClaimCollection claims,
            ClaimsParameters parameters) {
        String[] attributes = {groupNameAttribute, memberNameAttribute};
        ClaimCollection claimsColl = new ClaimCollection();
        try {
            Principal principal = parameters.getPrincipal();

            String user = null;
            if (principal instanceof KerberosPrincipal) {
                KerberosPrincipal kp = (KerberosPrincipal) principal;
                StringTokenizer st = new StringTokenizer(kp.getName(), "@");
                user = st.nextToken();
            } else if (principal instanceof X500Principal) {
                // 1.2.840.113549.1.9.1=#160d69346365406c6d636f2e636f6d,CN=client,OU=I4CE,O=Lockheed
                // Martin,L=Goodyear,ST=Arizona,C=US
                X500Principal x500p = (X500Principal)principal;
                StringTokenizer st = new StringTokenizer(x500p.getName(), ",");
                while(st.hasMoreElements())
                {
                    //token is in the format:
                    //syntaxAndUniqueId
                    //cn
                    //ou
                    //o
                    //loc
                    //state
                    //country
                    String[] strArr = st.nextToken().split("=");
                    if(strArr.length > 1 && strArr[0].equalsIgnoreCase("cn"))
                    {
                        user = strArr[1];
                        break;
                    }
                }
            } else if (principal != null) {
                user = principal.getName();
            } else {
                logger.info("Principal is null");
                return new ClaimCollection();
            }

            if (user == null) {
                logger.warn("User must not be null");
                return new ClaimCollection();
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("Retrieve role claims for user " + user);
                }
            }

            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectClass", getObjectClass())).and(
                    new EqualsFilter(getMemberNameAttribute(),
                            getUserNameAttribute() + "=" + user + ","
                                    + getUserBaseDn()));

            AttributesMapper mapper = new AttributesMapper() {
                public Object mapFromAttributes(Attributes attrs)
                        throws NamingException {
                    Map<String, Attribute> map = new HashMap<String, Attribute>();
                    NamingEnumeration<? extends Attribute> attrEnum = attrs
                            .getAll();
                    while (attrEnum.hasMore()) {
                        Attribute att = attrEnum.next();
                        map.put(att.getID(), att);
                    }
                    return map;
                }
            };

            List<?> results = ldapTemplate.search(groupBaseDn,
                    filter.toString(), SearchControls.SUBTREE_SCOPE,
                    attributes, mapper);

            for (Object result : results) {
                Map<String, Attribute> ldapAttributes = null;
                ldapAttributes = CastUtils.cast((Map<?, ?>) result);

                Attribute attr = ldapAttributes.get(groupNameAttribute);
                if (attr == null) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Claim '" + roleClaimType + "' is null");
                    }
                } else {
                    Claim c = new Claim();
                    c.setClaimType(getRoleURI());
                    c.setPrincipal(principal);

                    StringBuilder claimValue = new StringBuilder();
                    try {
                        NamingEnumeration<?> list = (NamingEnumeration<?>) attr
                                .getAll();
                        while (list.hasMore()) {
                            Object obj = list.next();
                            if (!(obj instanceof String)) {
                                logger.warn("LDAP attribute '"
                                        + groupNameAttribute
                                        + "' has got an unsupported value type");
                                break;
                            }
                            claimValue.append((String) obj);
                            if (list.hasMore()) {
                                claimValue.append(getDelimiter());
                            }
                        }
                    } catch (NamingException ex) {
                        logger.warn("Failed to read value of LDAP attribute '"
                                + groupNameAttribute + "'");
                    }

                    c.setValue(claimValue.toString());
                    // c.setIssuer(issuer);
                    // c.setOriginalIssuer(originalIssuer);
                    // c.setNamespace(namespace);
                    claimsColl.add(c);
                }
            }
        } catch (Exception e) {
            logger.error("Unable to set role claims.", e);
        }
        return claimsColl;
    }

}

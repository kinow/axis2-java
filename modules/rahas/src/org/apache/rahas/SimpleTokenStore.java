/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rahas;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * In-memory implementation of the token storage
 */
public class SimpleTokenStore implements TokenStorage {

    protected Map tokens = new Hashtable();

    public void add(Token token) throws TrustException {
        if (token != null && !"".equals(token.getId()) &&
            token.getId() != null) {
            if (this.tokens.keySet().size() == 0
                || (this.tokens.keySet().size() > 0 && !this.tokens
                    .keySet().contains(token.getId()))) {
                tokens.put(token.getId(), token);
            } else {
                throw new TrustException("tokenAlreadyExists",
                                         new String[]{token.getId()});
            }

        }
    }

    public void update(Token token) throws TrustException {
        if (token != null && token.getId() != null && token.getId().trim().length() != 0) {

            if (!this.tokens.keySet().contains(token.getId())) {
                throw new TrustException("noTokenToUpdate", new String[]{token.getId()});
            }
            this.tokens.put(token.getId(), token);
        }
    }

    public String[] getTokenIdentifiers() throws TrustException {
        List identifiers = new ArrayList();
        for (Iterator iterator = tokens.keySet().iterator(); iterator.hasNext();) {
            identifiers.add(iterator.next());
        }
        return (String[]) identifiers.toArray(new String[identifiers.size()]);
    }

    public List getValidTokens() throws TrustException {
        return getTokens(new int[]{Token.ISSUED, Token.RENEWED});
    }

    public List getRenewedTokens() throws TrustException {
        return getTokens(Token.RENEWED);
    }


    public List getCancelledTokens() throws TrustException {
        return getTokens(Token.CANCELLED);
    }

    public List getExpiredTokens() throws TrustException {
        return getTokens(Token.EXPIRED);
    }

    private List getTokens(int[] states) throws TrustException {
        processTokenExpiry();
        List tokens = new ArrayList();
        for (Iterator iterator = this.tokens.values().iterator(); iterator.hasNext();) {
            Token token = (Token) iterator.next();
            for (int i = 0; i < states.length; i++) {
                if (token.getState() == states[i]) {
                    tokens.add(token);
                    break;
                }
            }
        }
        return tokens;
    }

    private List getTokens(int state) throws TrustException {
        processTokenExpiry();
        List tokens = new ArrayList();
        for (Iterator iterator = this.tokens.values().iterator(); iterator.hasNext();) {
            Token token = (Token) iterator.next();
            if (token.getState() == state) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    public Token getToken(String id) throws TrustException {
        return (Token) this.tokens.get(id);
    }

    protected void processTokenExpiry() throws TrustException {
        for (Iterator iterator = tokens.values().iterator(); iterator.hasNext();) {
            Token token = (Token) iterator.next();
            if (token.getExpires() != null &&
                token.getExpires().getTime() < System.currentTimeMillis()) {
                token.setState(Token.EXPIRED);
                update(token);
            }
        }
    }
}

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

package org.apache.ws.commons.soap;

public class SOAPFaultReasonTestCase extends SOAPFaultTestCase {

    protected SOAPFaultReason soap11FaultReason;
    protected SOAPFaultReason soap12FaultReason;
    protected SOAPFaultReason soap11FaultReasonWithParser;
    protected SOAPFaultReason soap12FaultReasonWithParser;

    public SOAPFaultReasonTestCase(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        super.setUp();
        soap11FaultReason = soap11Factory.createSOAPFaultReason(soap11Fault);
        soap12FaultReason = soap12Factory.createSOAPFaultReason(soap12Fault);
        soap11FaultReasonWithParser = soap11FaultWithParser.getReason();
        soap12FaultReasonWithParser = soap12FaultWithParser.getReason();
    }

}

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

package org.apache.axis2.security;
/**
 * WS-Security interop scenario 4
 * @author Ruchith Fernando (ruchith.fernando@gmail.com)
 */
public class Scenario4Test extends InteropTestBase {
	
	protected void setUp() throws Exception {
		this.setClientRepo(SCENARIO3_CLIENT_REPOSITORY);
		this.setServiceRepo(SCENARIO3_SERVICE_REPOSITORY);
		super.setUp();
	}

}

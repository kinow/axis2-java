/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axis2.clustering.tribes;

import org.apache.axis2.clustering.ClusteringConstants;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.control.GetConfigurationCommand;
import org.apache.axis2.clustering.control.GetConfigurationResponseCommand;
import org.apache.axis2.clustering.control.GetStateCommand;
import org.apache.axis2.clustering.control.GetStateResponseCommand;
import org.apache.axis2.clustering.control.wka.JoinGroupCommand;
import org.apache.axis2.clustering.control.wka.MemberJoinedCommand;
import org.apache.axis2.clustering.control.wka.MemberListCommand;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.RemoteProcessException;
import org.apache.catalina.tribes.group.RpcCallback;
import org.apache.catalina.tribes.group.interceptors.StaticMembershipInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;

/**
 * Handles RPC Channel requests from members
 */
public class RpcRequestHandler implements RpcCallback {

    private static Log log = LogFactory.getLog(RpcRequestHandler.class);
    private ConfigurationContext configurationContext;
    private MembershipManager membershipManager;
    private StaticMembershipInterceptor staticMembershipInterceptor;

    public RpcRequestHandler(ConfigurationContext configurationContext,
                             MembershipManager membershipManager,
                             StaticMembershipInterceptor staticMembershipInterceptor) {
        this.configurationContext = configurationContext;
        this.membershipManager = membershipManager;
        this.staticMembershipInterceptor = staticMembershipInterceptor;
    }

    public void setConfigurationContext(ConfigurationContext configurationContext) {
        this.configurationContext = configurationContext;
    }

    public Serializable replyRequest(Serializable msg, Member invoker) {
        if (msg instanceof GetStateCommand) {
            // If a GetStateRequest is received by a node which has not yet initialized
            // this node cannot send a response to the state requester. So we simply return.
            if (configurationContext.
                    getPropertyNonReplicable(ClusteringConstants.CLUSTER_INITIALIZED) == null) {
                return null;
            }
            try {
                log.info("Received " + msg + " initialization request message from " +
                         TribesUtil.getHost(invoker));
                GetStateCommand command = (GetStateCommand) msg;
                command.execute(configurationContext);
                GetStateResponseCommand getStateRespCmd = new GetStateResponseCommand();
                getStateRespCmd.setCommands(command.getCommands());
                return getStateRespCmd;
            } catch (ClusteringFault e) {
                String errMsg = "Cannot handle initialization request";
                log.error(errMsg, e);
                throw new RemoteProcessException(errMsg, e);
            }
        } else if (msg instanceof GetConfigurationCommand) {
            // If a GetConfigurationCommand is received by a node which has not yet initialized
            // this node cannot send a response to the state requester. So we simply return.
            if (configurationContext.
                    getPropertyNonReplicable(ClusteringConstants.CLUSTER_INITIALIZED) == null) {
                return null;
            }
            try {
                log.info("Received " + msg + " initialization request message from " +
                         TribesUtil.getHost(invoker));
                GetConfigurationCommand command = (GetConfigurationCommand) msg;
                command.execute(configurationContext);
                GetConfigurationResponseCommand
                        getConfigRespCmd = new GetConfigurationResponseCommand();
                getConfigRespCmd.setServiceGroups(command.getServiceGroupNames());
                return getConfigRespCmd;
            } catch (ClusteringFault e) {
                String errMsg = "Cannot handle initialization request";
                log.error(errMsg, e);
                throw new RemoteProcessException(errMsg, e);
            }
        } else if (msg instanceof JoinGroupCommand) {
            log.info("Received JOIN message from " + TribesUtil.getHost(invoker));
            MemberListCommand memListCmd;
            try {
                // Add the member
                staticMembershipInterceptor.memberAdded(invoker);
                membershipManager.memberAdded(invoker);

                // Return the list of current members to the caller
                memListCmd = new MemberListCommand();
                memListCmd.setMembers(membershipManager.getMembers());
            } catch (Exception e) {
                String errMsg = "Cannot handle JOIN request";
                log.error(errMsg, e);
                throw new RemoteProcessException(errMsg, e);
            }
            return memListCmd;
        } else if (msg instanceof MemberJoinedCommand) {
            log.info("Received MEMBER_JOINED message from " + TribesUtil.getHost(invoker));
            try {
                MemberJoinedCommand command = (MemberJoinedCommand) msg;
                command.setMembershipManager(membershipManager);
                command.setStaticMembershipInterceptor(staticMembershipInterceptor);
                command.execute(configurationContext);
            } catch (ClusteringFault e) {
                String errMsg = "Cannot handle MEMBER_JOINED notification";
                log.error(errMsg, e);
                throw new RemoteProcessException(errMsg, e);
            }
        } else if (msg instanceof MemberListCommand) {
            try {                    //TODO: What if we receive more than one member list message?
                MemberListCommand command = (MemberListCommand) msg;
                command.setMembershipManager(membershipManager);
                command.setStaticMembershipInterceptor(staticMembershipInterceptor);
                command.setSender(invoker);
                command.execute(configurationContext);

                //TODO Send MEMBER_JOINED messages to all nodes
            } catch (ClusteringFault e) {
                String errMsg = "Cannot handle MEMBER_LIST message";
                log.error(errMsg, e);
                throw new RemoteProcessException(errMsg, e);
            }
        }
        return null;                      
    }

    public void leftOver(Serializable msg, Member member) {
        //TODO: Method implementation

    }
}

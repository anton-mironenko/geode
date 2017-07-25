/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.management.internal.cli.functions;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Map;

import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.distributed.internal.DistributionConfig;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.internal.InternalEntity;
import org.apache.geode.internal.cache.CacheClientStatus;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.tier.InternalClientMembership;
import org.apache.geode.internal.cache.tier.sockets.ClientProxyMembershipID;
import org.apache.geode.management.internal.cli.CliUtil;
import org.apache.geode.management.internal.cli.domain.CacheServerInfo;
import org.apache.geode.management.internal.cli.domain.MemberInformation;

/**
 * @since GemFire 7.0
 */
public class GetMemberInformationFunction implements Function, InternalEntity {
  private static final long serialVersionUID = 1L;

  @Override
  public void execute(final FunctionContext functionContext) {
    try {
      InternalCache cache = (InternalCache) functionContext.getCache();

      // TODO: 1) Get the CPU usage%

      InternalDistributedSystem system = cache.getInternalDistributedSystem();
      DistributionConfig config = system.getConfig();
      String serverBindAddress = config.getServerBindAddress();
      MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

      MemberInformation memberInfo = new MemberInformation();
      memberInfo.setGroups(config.getGroups());
      memberInfo.setLogFilePath(config.getLogFile().getCanonicalPath());
      memberInfo.setStatArchiveFilePath(config.getStatisticArchiveFile().getCanonicalPath());
      memberInfo.setWorkingDirPath(System.getProperty("user.dir"));
      memberInfo.setCacheXmlFilePath(config.getCacheXmlFile().getCanonicalPath());
      memberInfo.setLocators(config.getLocators());
      memberInfo.setServerBindAddress(serverBindAddress);
      memberInfo.setOffHeapMemorySize(config.getOffHeapMemorySize());

      MemoryUsage memUsage = memoryMXBean.getHeapMemoryUsage();
      memberInfo.setHeapUsage(Long.toString(bytesToMeg(memUsage.getUsed())));
      memberInfo.setMaxHeapSize(Long.toString(bytesToMeg(memUsage.getMax())));
      memberInfo.setInitHeapSize(Long.toString(bytesToMeg(memUsage.getInit())));
      memberInfo.setHostedRegions(CliUtil.getAllRegionNames());

      List<CacheServer> csList = cache.getCacheServers();

      // A member is a server only if it has a cacheserver
      if (csList != null) {
        memberInfo.setServer(true);
        for (CacheServer cs : csList) {
          String bindAddress = cs.getBindAddress();
          int port = cs.getPort();
          boolean isRunning = cs.isRunning();

          CacheServerInfo cacheServerInfo = new CacheServerInfo(bindAddress, port, isRunning);
          memberInfo.addCacheServerInfo(cacheServerInfo);
        }

        Map<ClientProxyMembershipID, CacheClientStatus> allConnectedClients =
            InternalClientMembership.getStatusForAllClientsIgnoreSubscriptionStatus();

        int numConnections = 0;
        for (ClientProxyMembershipID clientProxyMembershipID : allConnectedClients.keySet()) {
          CacheClientStatus status = allConnectedClients.get(clientProxyMembershipID);
          numConnections = numConnections + status.getNumberOfConnections();
        }

        memberInfo.setClientCount(numConnections);

      } else {
        memberInfo.setServer(false);
      }

      functionContext.getResultSender().lastResult(memberInfo);

    } catch (CacheClosedException e) {
      functionContext.getResultSender().sendException(e);

    } catch (Exception e) {
      functionContext.getResultSender().sendException(e);
    }
  }

  @Override
  public boolean hasResult() {
    return true;
  }

  @Override
  public boolean isHA() {
    return true;
  }

  @Override
  public boolean optimizeForWrite() {
    return false;
  }

  private long bytesToMeg(final long bytes) {
    return bytes / (1024L * 1024L);
  }

}

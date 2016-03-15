/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package writeonly.offheap;

import io.rainfall.Runner;
import io.rainfall.Scenario;
import io.rainfall.ScenarioRun;
import io.rainfall.configuration.ConcurrencyConfig;
import io.rainfall.ehcache.statistics.EhcacheResult;
import io.rainfall.ehcache2.CacheConfig;
import io.rainfall.ehcache2.Ehcache2Operations;
import io.rainfall.unit.TimeDivision;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import utils.LongWrapper;
import utils.LongWrapperGenerator;
import utils.StringWrapper;
import utils.StringWrapperGenerator;

import java.io.File;

import static io.rainfall.configuration.ReportingConfig.html;
import static io.rainfall.configuration.ReportingConfig.report;
import static io.rainfall.execution.Executions.during;

public class Ehcache2_serializable {

  public static void main(String[] args) throws Exception {
    System.setProperty("com.tc.productkey.path", System.getProperty("user.home") + "/.tc/terracotta-license.key");
    Configuration configuration = new Configuration();
    CacheConfiguration cacheConfiguration = new CacheConfiguration("cache1", 1000);
    cacheConfiguration.setMaxBytesLocalOffHeap(MemoryUnit.parseSizeInBytes("2G"));
    configuration.addCache(cacheConfiguration);
    CacheManager cacheManager = new CacheManager(configuration);

    final Cache cache1 = cacheManager.getCache("cache1");

    LongWrapperGenerator keyGenerator = new LongWrapperGenerator();
    StringWrapperGenerator valueGenerator = new StringWrapperGenerator(4096);

    CacheConfig<Long, String> cacheConfig = new CacheConfig<Long, String>();
    cacheConfig.caches(cache1);

    final File reportPath = new File("target/rainfall/" + Ehcache2_serializable.class.getName().replace('.', '/'));

    System.out.println("testing...");
    ScenarioRun scenarioRun = Runner.setUp(
        Scenario.scenario("Testing phase")
            .exec(
                Ehcache2Operations.put(LongWrapper.class, StringWrapper.class).using(keyGenerator, valueGenerator)
                    .sequentially()
            ))
        .executed(during(120, TimeDivision.seconds))
        .config(
            ConcurrencyConfig.concurrencyConfig().threads(Runtime.getRuntime().availableProcessors()),
            report(EhcacheResult.class, new EhcacheResult[]{EhcacheResult.PUT}).log(html(reportPath.getPath())),
            cacheConfig);


    scenarioRun.start();

    cacheManager.shutdown();

    System.exit(0);
  }

}

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
import io.rainfall.ehcache3.CacheConfig;
import io.rainfall.ehcache3.Ehcache3Operations;
import io.rainfall.unit.TimeDivision;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import utils.LongWrapper;
import utils.LongWrapperGenerator;
import utils.StringWrapper;
import utils.StringWrapperGenerator;

import java.io.File;

import static io.rainfall.configuration.ReportingConfig.html;
import static io.rainfall.configuration.ReportingConfig.report;
import static io.rainfall.execution.Executions.during;
import static org.ehcache.config.builders.ResourcePoolsBuilder.heap;

public class Ehcache3_serializable {

  public static void main(String[] args) throws Exception {
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .withCache("cache1", CacheConfigurationBuilder.newCacheConfigurationBuilder(LongWrapper.class, StringWrapper.class, heap(1000).offheap(2, MemoryUnit.GB))
            .withResourcePools(ResourcePoolsBuilder.newResourcePoolsBuilder()
                )
            .build())
        .build(true);

    final Cache<LongWrapper, StringWrapper> cache1 = cacheManager.getCache("cache1", LongWrapper.class, StringWrapper.class);

    LongWrapperGenerator keyGenerator = new LongWrapperGenerator();
    StringWrapperGenerator valueGenerator = new StringWrapperGenerator(4096);

    CacheConfig<LongWrapper, StringWrapper> cacheConfig = new CacheConfig<LongWrapper, StringWrapper>();
    cacheConfig.cache("cache1", cache1);

    final File reportPath = new File("target/rainfall/" + Ehcache3_serializable.class.getName().replace('.', '/'));

    System.out.println("testing...");
    ScenarioRun scenarioRun = Runner.setUp(
        Scenario.scenario("Testing phase")
            .exec(
                Ehcache3Operations.put(LongWrapper.class, StringWrapper.class).using(keyGenerator, valueGenerator)
                    .sequentially()
            ))
        .executed(during(120, TimeDivision.seconds))
        .config(
            ConcurrencyConfig.concurrencyConfig().threads(Runtime.getRuntime().availableProcessors()),
            report(EhcacheResult.class, new EhcacheResult[]{EhcacheResult.PUT}).log(html(reportPath.getPath())),
            cacheConfig);


    scenarioRun.start();

    cacheManager.close();

    System.exit(0);
  }

}

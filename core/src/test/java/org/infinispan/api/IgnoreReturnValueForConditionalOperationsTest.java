package org.infinispan.api;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.test.MultipleCacheManagersTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * https://issues.jboss.org/browse/ISPN-3141
 */
@Test(groups = "functional", testName = "api.IgnoreReturnValueForConditionalOperationsTest")
public class IgnoreReturnValueForConditionalOperationsTest extends MultipleCacheManagersTest {

   protected boolean transactional;

   @Override
   protected void createCacheManagers() throws Throwable {
      transactional = false;
      ConfigurationBuilder dcc = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, transactional);
      createCluster(dcc, 2);
      waitForClusterToForm();
   }


   public void testConditionalReplace() {
      Object k = init();
      assertTrue(advancedCache(0).withFlags(Flag.IGNORE_RETURN_VALUES).replace(k, "v0", "v1"));
      assertEquals(cache(0).get(k), "v1");
      assertEquals(cache(1).get(k), "v1");
   }

   public void testConditionalRemove() {
      Object k = init();
      assertTrue(advancedCache(0).withFlags(Flag.IGNORE_RETURN_VALUES).remove(k, "v0"));
      assertNull(cache(0).get(k));
      assertNull(cache(1).get(k));
   }

   private Object init() {
      Object k = getKeyForCache(1);
      cache(0).put(k, "v0");
      assertEquals(cache(0).get(k), "v0");
      assertEquals(cache(1).get(k), "v0");
      return k;
   }
}

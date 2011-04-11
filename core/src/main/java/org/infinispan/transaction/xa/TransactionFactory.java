package org.infinispan.transaction.xa;

import org.infinispan.commands.write.WriteCommand;
import org.infinispan.config.Configuration;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.transaction.LocalTransaction;
import org.infinispan.transaction.RemoteTransaction;
import org.infinispan.transaction.synchronization.SyncLocalTransaction;
import org.infinispan.transaction.xa.recovery.RecoveryAwareDldGlobalTransaction;
import org.infinispan.transaction.xa.recovery.RecoveryAwareRemoteTransaction;
import org.infinispan.transaction.xa.recovery.RecoveryAwareGlobalTransaction;
import org.infinispan.transaction.xa.recovery.RecoveryAwareLocalTransaction;
import org.infinispan.util.ClusterIdGenerator;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import javax.transaction.Transaction;
import java.util.Random;

/**
 * Factory for transaction related sate.
 *
 * @author Mircea.Markus@jboss.com
 */
public class TransactionFactory {

   private static final Log log = LogFactory.getLog(TransactionFactory.class);

   private TxFactoryEnum txFactoryEnum;

   private EmbeddedCacheManager cm;
   private Configuration configuration;
   private boolean recoveryEnabled;
   private ClusterIdGenerator clusterIdGenerator;
   private boolean isClustered;
   private RpcManager rpcManager;

   private enum TxFactoryEnum {

      DLD_RECOVERY_XA {
         @Override
         public LocalTransaction newLocalTransaction(Transaction tx, GlobalTransaction gtx) {
            return new RecoveryAwareLocalTransaction(tx, gtx);
         }

         @Override
         public GlobalTransaction newGlobalTransaction(Address addr, boolean remote, ClusterIdGenerator clusterIdGenerator, boolean clustered) {
            RecoveryAwareDldGlobalTransaction dldGlobalTransaction = new RecoveryAwareDldGlobalTransaction(addr, remote);
            dldGlobalTransaction.setInternalId(clusterIdGenerator.newVersion(clustered));
            return addCoinToss(dldGlobalTransaction);
         }

         @Override
         public GlobalTransaction newGlobalTransaction() {
            return new RecoveryAwareDldGlobalTransaction();
         }

         @Override
         public RemoteTransaction newRemoteTransaction(WriteCommand[] modifications, GlobalTransaction tx) {
            return new RecoveryAwareRemoteTransaction(modifications, tx);
         }

         @Override
         public RemoteTransaction newRemoteTransaction(GlobalTransaction tx) {
            return new RecoveryAwareRemoteTransaction(tx);
         }
      },

      DLD_NORECOVERY_XA {
         @Override
         public LocalTransaction newLocalTransaction(Transaction tx, GlobalTransaction gtx) {
            return new LocalXaTransaction(tx, gtx);
         }

         @Override
         public GlobalTransaction newGlobalTransaction(Address addr, boolean remote, ClusterIdGenerator clusterIdGenerator, boolean clustered) {
            return addCoinToss(new DldGlobalTransaction(addr, remote));
         }

         @Override
         public GlobalTransaction newGlobalTransaction() {
            return new DldGlobalTransaction();
         }

         @Override
         public RemoteTransaction newRemoteTransaction(WriteCommand[] modifications, GlobalTransaction tx) {
            return new RemoteTransaction(modifications, tx);
         }

         @Override
         public RemoteTransaction newRemoteTransaction(GlobalTransaction tx) {
            return new RemoteTransaction(tx);
         }
      },

      DLD_NORECOVERY_NOXA {
         @Override
         public LocalTransaction newLocalTransaction(Transaction tx, GlobalTransaction gtx) {
            return new SyncLocalTransaction(tx, gtx);
         }

         @Override
         public GlobalTransaction newGlobalTransaction(Address addr, boolean remote, ClusterIdGenerator clusterIdGenerator, boolean clustered) {
            return addCoinToss(new DldGlobalTransaction(addr, remote));
         }

         @Override
         public GlobalTransaction newGlobalTransaction() {
            return new DldGlobalTransaction();
         }

         @Override
         public RemoteTransaction newRemoteTransaction(WriteCommand[] modifications, GlobalTransaction tx) {
            return new RemoteTransaction(modifications, tx);
         }

         @Override
         public RemoteTransaction newRemoteTransaction(GlobalTransaction tx) {
            return new RemoteTransaction(tx);
         }
      },
      NODLD_RECOVERY_XA {
         @Override
         public LocalTransaction newLocalTransaction(Transaction tx, GlobalTransaction gtx) {
            return new RecoveryAwareLocalTransaction(tx, gtx);
         }

         @Override
         public GlobalTransaction newGlobalTransaction(Address addr, boolean remote, ClusterIdGenerator clusterIdGenerator, boolean clustered) {
            RecoveryAwareGlobalTransaction recoveryAwareGlobalTransaction = new RecoveryAwareGlobalTransaction(addr, remote);
            recoveryAwareGlobalTransaction.setInternalId(clusterIdGenerator.newVersion(clustered));
            return recoveryAwareGlobalTransaction;
         }

         @Override
         public GlobalTransaction newGlobalTransaction() {
            return new RecoveryAwareGlobalTransaction();
         }

         @Override
         public RemoteTransaction newRemoteTransaction(WriteCommand[] modifications, GlobalTransaction tx) {
            return new RecoveryAwareRemoteTransaction(modifications, tx);
         }

         @Override
         public RemoteTransaction newRemoteTransaction(GlobalTransaction tx) {
            return new RecoveryAwareRemoteTransaction(tx);
         }
      },
      NODLD_NORECOVERY_XA {
         @Override
         public LocalTransaction newLocalTransaction(Transaction tx, GlobalTransaction gtx) {
            return new LocalXaTransaction(tx, gtx);
         }

         @Override
         public GlobalTransaction newGlobalTransaction(Address addr, boolean remote, ClusterIdGenerator clusterIdGenerator, boolean clustered) {
            return new GlobalTransaction(addr, remote);
         }

         @Override
         public GlobalTransaction newGlobalTransaction() {
            return new GlobalTransaction();
         }

         @Override
         public RemoteTransaction newRemoteTransaction(WriteCommand[] modifications, GlobalTransaction tx) {
            return new RemoteTransaction(modifications, tx);
         }

         @Override
         public RemoteTransaction newRemoteTransaction(GlobalTransaction tx) {
            return new RemoteTransaction(tx);
         }
      },
      NODLD_NORECOVERY_NOXA {
         @Override
         public LocalTransaction newLocalTransaction(Transaction tx, GlobalTransaction gtx) {
            return new SyncLocalTransaction(tx, gtx);
         }

         @Override
         public GlobalTransaction newGlobalTransaction(Address addr, boolean remote, ClusterIdGenerator clusterIdGenerator, boolean clustered) {
            return new GlobalTransaction(addr, remote);
         }

         @Override
         public GlobalTransaction newGlobalTransaction() {
            return new GlobalTransaction();
         }

         @Override
         public RemoteTransaction newRemoteTransaction(WriteCommand[] modifications, GlobalTransaction tx) {
            return new RemoteTransaction(modifications, tx);
         }

         @Override
         public RemoteTransaction newRemoteTransaction(GlobalTransaction tx) {
            return new RemoteTransaction(tx);
         }
      };


      public abstract LocalTransaction newLocalTransaction(Transaction tx, GlobalTransaction gtx);
      public abstract GlobalTransaction newGlobalTransaction(Address addr, boolean remote, ClusterIdGenerator clusterIdGenerator, boolean clustered);
      public abstract GlobalTransaction newGlobalTransaction();

      protected long generateRandomId() {
         return rnd.nextLong();
      }

      protected GlobalTransaction addCoinToss(DldGlobalTransaction dldGlobalTransaction) {
         dldGlobalTransaction.setCoinToss(generateRandomId());
         return dldGlobalTransaction;
      }

      /**
       * this class is internally synchronized, so it can be shared between instances
       */
      private final Random rnd = new Random();

      public abstract RemoteTransaction newRemoteTransaction(WriteCommand[] modifications, GlobalTransaction tx);

      public abstract RemoteTransaction newRemoteTransaction(GlobalTransaction tx);
   }


   public GlobalTransaction newGlobalTransaction() {
      return txFactoryEnum.newGlobalTransaction();
   }

   public GlobalTransaction newGlobalTransaction(Address addr, boolean remote) {
      return txFactoryEnum.newGlobalTransaction(addr, remote, this.clusterIdGenerator, isClustered);
   }

   public LocalTransaction newLocalTransaction(Transaction tx, GlobalTransaction gtx) {
      return txFactoryEnum.newLocalTransaction(tx, gtx);
   }

   public RemoteTransaction newRemoteTransaction(WriteCommand[] modifications, GlobalTransaction tx) {
      return txFactoryEnum.newRemoteTransaction(modifications, tx);
   }

   public RemoteTransaction newRemoteTransaction(GlobalTransaction tx) {
      return txFactoryEnum.newRemoteTransaction(tx);
   }


   public TransactionFactory() {
      init(false, false, true);
   }

   public TransactionFactory(boolean dldEnabled) {
      init(dldEnabled, false, true);
   }

   public TransactionFactory(boolean dldEnabled, boolean recoveryEnabled) {
      init(dldEnabled, recoveryEnabled, true);
   }


   @Inject
   public void init(Configuration configuration, EmbeddedCacheManager cm, RpcManager rpcManager) {
      this.cm = cm;
      this.configuration = configuration;
      this.rpcManager = rpcManager;
   }

   @Start
   public void start() {
      boolean dldEnabled = configuration.isEnableDeadlockDetection();
      boolean xa = !configuration.isUseSynchronizationForTransactions();
      recoveryEnabled = configuration.isTransactionRecoveryEnabled();
      init(dldEnabled, recoveryEnabled, xa);
      isClustered = configuration.getCacheMode().isClustered();
      if (recoveryEnabled) {
         clusterIdGenerator = new ClusterIdGenerator();
         ClusterIdGenerator.RankCalculator rcl = clusterIdGenerator.getRankCalculatorListener();
         cm.addListener(rcl);
         if (rpcManager != null) {
            Transport transport = rpcManager.getTransport();
            rcl.calculateRank(rpcManager.getAddress(), transport.getMembers(), transport.getViewId());
         }
      }
   }

   private void init(boolean dldEnabled, boolean recoveryEnabled, boolean xa) {
      if (dldEnabled && recoveryEnabled && xa) {
         txFactoryEnum = TxFactoryEnum.DLD_RECOVERY_XA;
      } else if (dldEnabled && !recoveryEnabled && xa) {
         txFactoryEnum = TxFactoryEnum.DLD_NORECOVERY_XA;
      } else if (dldEnabled && !recoveryEnabled && !xa) {
         txFactoryEnum = TxFactoryEnum.DLD_NORECOVERY_NOXA;
      } else  if (!dldEnabled && recoveryEnabled && xa) {
         txFactoryEnum = TxFactoryEnum.NODLD_RECOVERY_XA;
      } else if (!dldEnabled && !recoveryEnabled && xa) {
         txFactoryEnum = TxFactoryEnum.NODLD_NORECOVERY_XA;
      } else if (!dldEnabled && !recoveryEnabled && !xa) {
         txFactoryEnum = TxFactoryEnum.NODLD_NORECOVERY_NOXA;
      }
      if (log.isTraceEnabled()) log.trace("Setting factory enum to %s", txFactoryEnum);

      if (txFactoryEnum == null) {
         log.error("Unsupported combination (dldEnabled, recoveryEnabled, xa) = (%s, %s, %s)", dldEnabled, recoveryEnabled, xa);
         throw new IllegalStateException("Unsupported combination (dldEnabled, recoveryEnabled, xa) = (" + dldEnabled
                                               + ", " + recoveryEnabled + ", " + xa + ")");
      }
   }
}
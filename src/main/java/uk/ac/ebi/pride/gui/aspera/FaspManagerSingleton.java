package uk.ac.ebi.pride.gui.aspera;

import com.asperasoft.faspmanager.FaspManager;
import com.asperasoft.faspmanager.FaspManagerException;
import com.asperasoft.faspmanager.TransferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe singleton wrapper for FaspManager to prevent race conditions
 * and authentication issues.
 */
public class FaspManagerSingleton {
    
    private static final Logger logger = LoggerFactory.getLogger(FaspManagerSingleton.class);
    private static volatile FaspManagerSingleton instance;
    private static final ReentrantLock lock = new ReentrantLock();
    
    private volatile FaspManager faspManager;
    private volatile boolean initialized = false;
    
    private FaspManagerSingleton() {
        // Private constructor
    }
    
    public static FaspManagerSingleton getInstance() {
        if (instance == null) {
            synchronized (FaspManagerSingleton.class) {
                if (instance == null) {
                    instance = new FaspManagerSingleton();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get the FaspManager instance, initializing if necessary
     */
    public FaspManager getFaspManager() throws FaspManagerException {
        if (!initialized) {
            lock.lock();
            try {
                if (!initialized) {
                    logger.info("Initializing FaspManager singleton");
                    faspManager = FaspManager.getSingleton();
                    initialized = true;
                    logger.info("FaspManager singleton initialized successfully");
                }
            } finally {
                lock.unlock();
            }
        }
        return faspManager;
    }
    
    /**
     * Add a listener to FaspManager
     */
    public void addListener(TransferListener listener) throws FaspManagerException {
        lock.lock();
        try {
            FaspManager manager = getFaspManager();
            manager.addListener(listener);
            logger.debug("Added listener to FaspManager");
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Remove a listener from FaspManager
     */
    public void removeListener(TransferListener listener) throws FaspManagerException {
        lock.lock();
        try {
            FaspManager manager = getFaspManager();
            manager.removeListener(listener);
            logger.debug("Removed listener from FaspManager");
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Start a transfer with proper synchronization
     */
    public String startTransfer(com.asperasoft.faspmanager.TransferOrder order) throws FaspManagerException {
        lock.lock();
        try {
            FaspManager manager = getFaspManager();
            String transferId = manager.startTransfer(order);
            logger.info("Started transfer with ID: {}", transferId);
            return transferId;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Check if FaspManager is running
     */
    public boolean isRunning() throws FaspManagerException {
        lock.lock();
        try {
            FaspManager manager = getFaspManager();
            return manager.isRunning();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Get session stats with proper synchronization
     */
    public com.asperasoft.faspmanager.SessionStats getSessionStats(String sessionId) throws FaspManagerException {
        lock.lock();
        try {
            FaspManager manager = getFaspManager();
            return manager.getSessionStats(sessionId);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Reset the FaspManager (for testing or error recovery)
     */
    public void reset() {
        lock.lock();
        try {
            logger.warn("Resetting FaspManager singleton");
            initialized = false;
            faspManager = null;
        } finally {
            lock.unlock();
        }
    }
}

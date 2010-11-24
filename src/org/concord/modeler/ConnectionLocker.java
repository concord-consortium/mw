package org.concord.modeler;

import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConnectionLocker {
	public static enum Mode {READ, WRITE};
	private static HashMap<URL, ReentrantReadWriteLock> locks = new HashMap<URL, ReentrantReadWriteLock>();
	
	public static void lock(URL url, Mode mode) {
		ReentrantReadWriteLock lock = findOrCreateLock(url);
		switch (mode) {
		case READ:
			lock.readLock().lock();
			break;
		case WRITE:
			lock.writeLock().lock();
			break;
		default:
			throw new RuntimeException("Invalid lock mode!");
		}
	}
	
	public static void unlock(URL url, Mode mode) {
		ReentrantReadWriteLock lock = findOrCreateLock(url);
		switch (mode) {
		case READ:
			lock.readLock().unlock();
			break;
		case WRITE:
			lock.writeLock().unlock();
			break;
		default:
			throw new RuntimeException("Invalid lock mode!");
		}
	}
	
	private static ReentrantReadWriteLock findOrCreateLock(URL url) {
		synchronized(locks) {
			if (locks.containsKey(url)) {
				return locks.get(url);
			}
			ReentrantReadWriteLock newLock = new ReentrantReadWriteLock();
			locks.put(url, newLock);
			return newLock;
		}
	}

	public static void lockAll(Mode mode) {
		synchronized(locks) {
			for (URL url : locks.keySet()) {
				lock(url, mode);
			}
		}
	}
	
	public static void unlockAll(Mode mode) {
		synchronized(locks) {
			for (URL url : locks.keySet()) {
				unlock(url, mode);
			}
		}
	}

}

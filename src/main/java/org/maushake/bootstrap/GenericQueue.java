/* 
 * %CopyrightBegin%
 * 
 * Copyright Ericsson AB 2000-2009. All Rights Reserved.
 * 
 * The contents of this file are subject to the Erlang Public License,
 * Version 1.1, (the "License"); you may not use this file except in
 * compliance with the License. You should have received a copy of the
 * Erlang Public License along with this software. If not, it can be
 * retrieved online at http://www.erlang.org/.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * %CopyrightEnd%
 * 
 */
package org.maushake.bootstrap;

/**
 * This class implements a generic FIFO queue. There is no upper bound on the length of the queue, items are linked.
 * 
 * <pre>
 *  jm140403 time slice machine parallel read
 *   this class's implementation is somewhat in-efficient as it takes no advantage of java atomics
 *   which it could do to completely avoid any synchronization (keyword synchronize)
 * </pre>
 */

public class GenericQueue<T> {
	private static final int open = 0;
	private static final int closing = 1;
	private static final int closed = 2;

	private int status;
	private Bucket<T> head;
	private Bucket<T> tail;
	private int count;

	private void init() {
		this.head = null;
		this.tail = null;
		this.count = 0;
	}

	/** Create an empty queue */
	public GenericQueue() {
		init();
		this.status = GenericQueue.open;
	}

	/** Clear a queue */
	public void flush() {
		init();
	}

	public void close() {
		this.status = GenericQueue.closing;
	}

	/**
	 * Add an object to the tail of the queue.
	 * 
	 * @param o
	 *            Object to insert in the queue
	 */
	public synchronized void put(final T o) {
		final Bucket<T> b = new Bucket<T>(o);

		if (this.tail != null) {
			this.tail.setNext(b);
			this.tail = b;
		} else {
			// queue was empty but has one element now
			this.head = this.tail = b;
		}
		this.count++;

		// notify any waiting tasks
		notify();
	}

	/**
	 * Retrieve an object from the head of the queue, or block until one arrives.
	 * 
	 * @return The object at the head of the queue.
	 */
	public synchronized T get() {
		T o;

		while ((o = tryGetImpl()) == null) {
			try {
				this.wait();
			} catch (final InterruptedException e) {
				return null;
			}
		}
		return o;
	}

	/**
	 * Retrieve an object from the head of the queue, blocking until one arrives or until timeout occurs.
	 * 
	 * @param timeout
	 *            Maximum time to block on queue, in ms. Use 0 to poll the queue.
	 * 
	 * @exception InterruptedException
	 *                if the operation times out.
	 * 
	 * @return The object at the head of the queue, or null if none arrived in time.
	 */
	public synchronized T get(final long timeout)
			throws InterruptedException {
		if (this.status == GenericQueue.closed) {
			return null;
		}

		long currentTime = System.currentTimeMillis();
		final long stopTime = currentTime + timeout;
		T o = null;

		while (true) {
			if ((o = tryGetImpl()) != null) {
				return o;
			}

			currentTime = System.currentTimeMillis();
			if (stopTime <= currentTime) {
				throw new InterruptedException("Get operation timed out");
			}

			try {
				this.wait(stopTime - currentTime);
			} catch (final InterruptedException e) {
				// ignore, but really should retry operation instead
			}
		}
	}

	/**
	 * Attempt to retrieve message from queue head
	 * 
	 * @return head of queue - non-blocking
	 * 
	 *         <pre>
	 *  o5131223 define swing terminals terminal adapter II
	 * </pre>
	 */
	private T tryGetImpl() {
		T o = null;

		if (this.head != null) {
			o = this.head.getContents();
			this.head = this.head.getNext();
			this.count--;

			if (this.head == null) {
				this.tail = null;
				this.count = 0;
			}
		}

		return o;
	}

	/**
	 * Attempt to retrieve message from queue head
	 * 
	 * @return head of queue - non-blocking
	 * 
	 *         <pre>
	 *  o5131223 define swing terminals terminal adapter II
	 * </pre>
	 */
	public synchronized T tryGet() {
		return tryGetImpl();
	}

	public synchronized int getCount() {
		return this.count;
	}

	/*
	 * The Bucket class. The queue is implemented as a linked list of Buckets. The container holds the queued object and
	 * a reference to the next Bucket.
	 */
	class Bucket<T> {
		private Bucket<T> next;
		private final T contents;

		public Bucket(final T o) {
			this.next = null;
			this.contents = o;
		}

		public void setNext(final Bucket newNext) {
			this.next = newNext;
		}

		public Bucket<T> getNext() {
			return this.next;
		}

		public T getContents() {
			return this.contents;
		}
	}
}

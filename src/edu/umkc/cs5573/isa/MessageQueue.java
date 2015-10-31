/**
 * 
 */
package edu.umkc.cs5573.isa;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * @author Younghwan
 *
 */
public class MessageQueue {
	private volatile static MessageQueue mMessageQueue;
	public static MessageQueue getInstance(){
		if(mMessageQueue == null){
			synchronized(MessageQueue.class){
				if(mMessageQueue == null){
					mMessageQueue = new MessageQueue();
				}
			}
		}
		return mMessageQueue;
	}
	private Queue<String> mQueue;
	private MessageQueue(){
		this.mQueue = new LinkedList<String>();
	}
	public String getFirstMessage() throws InterruptedException{
		if(mQueue.isEmpty()){
			return null;
		}
		return mQueue.peek();
	}
	
	public synchronized void queue(String item){
		mQueue.add(item);
	}
	
	public synchronized String deque() throws NoSuchElementException{
		return mQueue.remove();
	}
	
}

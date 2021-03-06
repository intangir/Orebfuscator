/*
 * Copyright (C) 2011-2012 lishid.  All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  version 3.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.lishid.orebfuscator.threading;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;


import org.bukkit.block.Block;

import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.OrebfuscatorConfig;
import com.lishid.orebfuscator.obfuscation.BlockUpdate;

public class OrebfuscatorThreadUpdate extends Thread implements Runnable
{
    // Global
    private static final int QUEUE_CAPACITY = 1024 * 10;
    private static final LinkedBlockingDeque<Block> queue = new LinkedBlockingDeque<Block>(QUEUE_CAPACITY);
    private static OrebfuscatorThreadUpdate thread;
    
    public static void terminate()
    {
        if (thread != null)
            thread.kill.set(true);
    }
    
    public static boolean needsUpdate(Block block)
    {
        byte id = (byte) block.getTypeId();
        return !OrebfuscatorConfig.isBlockTransparent(id);
    }
    
    public static void Queue(Block block)
    {
        if(!needsUpdate(block))
            return;
        
        if (!OrebfuscatorConfig.getUpdateThread())
        {
            BlockUpdate.UpdateBlocksNearby(block);
            return;
        }
        
        if (queue.contains(block))
        {
            return;
        }
        
        if (thread == null || thread.isInterrupted() || !thread.isAlive())
        {
            thread = new OrebfuscatorThreadUpdate();
            thread.setName("Orebfuscator Update Thread");
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
        
        while (true)
        {
            try
            {
                // Queue block for later processing
                queue.put(block);
                return;
            }
            catch (Exception e)
            {
                Orebfuscator.log(e);
            }
        }
    }
    
    private AtomicBoolean kill = new AtomicBoolean(false);
    
    public void run()
    {
        while (!this.isInterrupted() && !kill.get())
        {
            try
            {
                // Remove the first block from the queue
                Block block = queue.take();
                // Send updates on the block change
                BlockUpdate.UpdateBlocksNearby(block);
                
                // Exit if config changed to not using this thread.
                if (!OrebfuscatorConfig.getUpdateThread() && queue.size() <= 0)
                {
                    thread = null;
                    return;
                }
            }
            catch (Exception e)
            {
                Orebfuscator.log(e);
            }
        }
    }
}
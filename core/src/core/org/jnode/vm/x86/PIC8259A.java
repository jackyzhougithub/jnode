/*
 * $Id$
 */
package org.jnode.vm.x86;

import javax.naming.NameNotFoundException;

import org.jnode.naming.InitialNaming;
import org.jnode.system.IOResource;
import org.jnode.system.ResourceManager;
import org.jnode.system.ResourceNotFreeException;
import org.jnode.system.SimpleResourceOwner;
import org.jnode.vm.annotation.Inline;
import org.jnode.vm.annotation.KernelSpace;
import org.jnode.vm.annotation.PrivilegedActionPragma;
import org.jnode.vm.annotation.Uninterruptible;
import org.jnode.vm.scheduler.ProcessorLock;

/**
 * Wrapper for the 8259A Programmable Interrupt Controller.
 * 
 * @author Ewout Prangsma (epr@users.sourceforge.net)
 */
final class PIC8259A {

    /** PIC Access lock */
    private final ProcessorLock lock = new ProcessorLock();   
    
    // IO resources
    private final IOResource io8259_A;
    private final IOResource io8259_B;

    /**
     * Create and initialize.
     */
    @PrivilegedActionPragma
    public PIC8259A() {
        final SimpleResourceOwner owner = new SimpleResourceOwner("PIC8259A");
        
        try {
            final ResourceManager rm = (ResourceManager) InitialNaming.lookup(ResourceManager.NAME);
            io8259_A = rm.claimIOResource(owner, 0x20, 1); // 0xA0
            io8259_B = rm.claimIOResource(owner, 0xA0, 1);
        } catch (NameNotFoundException ex) {
            throw new Error("Cannot find ResourceManager", ex);
        } catch (ResourceNotFreeException ex) {
            throw new Error("Cannot claim PIC0-IO ports", ex);
        }
    }
    
    /**
     * Set an End Of Interrupt message to the 8259 interrupt controller(s).
     * 
     * @param irq
     */
    @Uninterruptible
    @KernelSpace
    @Inline
    final void eoi(int irq) {
        try {
            // Get access
            lock.lock();
            
            // Perform EOI
            if (irq < 8) {
                io8259_A.outPortByte(0x20, 0x60 + irq);
            } else {
                /*
                 * if (irq == 10) { Screen.debug(" <EOI 10/> ");
                 */
                io8259_B.outPortByte(0xA0, 0x60 + irq - 8);
                /* EOI of cascade */
                io8259_A.outPortByte(0x20, 0x60 + 2);
            }
        } finally {
            // Return access
            lock.unlock();
        }
    }
}

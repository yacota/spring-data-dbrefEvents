/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.model;

/**
 *
 * @author jllach
 */
public class Parent 
implements   AfterEvent {
    
    
    private boolean eventConvertReceived = false;
    
    
    @Override
    public void afterConvertEvent() {
        this.eventConvertReceived = true;
    }
    @Override
    public boolean isConvertEventCalled() {
        return eventConvertReceived;
    }
    
    
}

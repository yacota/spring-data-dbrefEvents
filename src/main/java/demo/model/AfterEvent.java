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
public interface AfterEvent {
    
    
    public void    afterConvertEvent();
    
    public boolean isConvertEventCalled();
    
}

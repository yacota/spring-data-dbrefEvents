/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo;

import com.mongodb.DBObject;
import demo.model.AfterEvent;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.stereotype.Component;

/**
 * @author jllach
 */
@Component
public class MongoEventListeener 
extends      AbstractMongoEventListener<AfterEvent> {

    private static int counter = 0;
    
    public static int   getAfetLoadEventsCounter() {
        return counter;
    }
    
    @Override
    public void onAfterLoad(DBObject dbo) {
        counter++;
    }
    
    @Override
    public void onAfterConvert(DBObject dbo, AfterEvent source) {
        source.afterConvertEvent();
    }
}

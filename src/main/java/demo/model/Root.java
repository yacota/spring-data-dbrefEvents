/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.model;

import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author jllach
 */
@Document
public class Root 
extends      Parent
implements   Serializable {
    private static final long serialVersionUID = -3211692873265644541L;
    
    @Id
    private Long id;
    
    private Related embed;
    @DBRef
    private Related ref;
    @DBRef(lazy = true)
    private Related lazyRef;
    
    @PersistenceConstructor
    public Root(Long id, Related embed, Related ref, Related lazyRef) {
        super();
        this.id = id;
        this.embed = embed;
        this.ref   = ref;
        this.lazyRef = lazyRef;
    }
    
    public Long getId() {
        return id;
    }

    public Related getEmbed() {
        return embed;
    }

    public Related getRef() {
        return ref;
    }

    public Related getLazyRef() {
        return lazyRef;
    }
}
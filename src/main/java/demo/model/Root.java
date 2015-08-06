/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.model;

import java.io.Serializable;
import java.util.List;
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
    
    // update adding list
    @DBRef
    private List<Related> listRef;
    @DBRef(lazy = true)
    private List<Related> listLazy;
    
    @PersistenceConstructor
    public Root(Long id, Related embed, Related ref, Related lazyRef, List<Related> listRef, List<Related> listLazy) {
        super();
        this.id = id;
        this.embed = embed;
        this.ref   = ref;
        this.lazyRef = lazyRef;
        this.listRef = listRef;
        this.listLazy = listLazy;
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

    public List<Related> getListRef() {
        return listRef;
    }

    public List<Related> getListLazy() {
        return listLazy;
    }
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.repository;

import demo.model.Root;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author jllach
 */
@Repository
public interface RootRepository
extends          CrudRepository<Root, Long> {
    
}

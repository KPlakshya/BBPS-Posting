package com.bbps.entity.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bbps.entity.CustomerRequestResponse;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRequestResponseRepo  extends JpaRepository<CustomerRequestResponse, Long>{

}

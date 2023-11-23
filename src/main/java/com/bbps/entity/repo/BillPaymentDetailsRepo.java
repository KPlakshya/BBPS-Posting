package com.bbps.entity.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bbps.entity.BillPaymentDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface BillPaymentDetailsRepo extends JpaRepository<BillPaymentDetails, Long>{

}

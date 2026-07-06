package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {

    @Modifying
    @Query("UPDATE Inventory i SET i.reserved = i.reserved + :qty WHERE i.item = :item AND (i.stock - i.reserved) >= :qty")
    int reserveStock(@Param("item") String item, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Inventory i SET i.stock = i.stock - :qty, i.reserved = i.reserved - :qty WHERE i.item = :item")
    int commitStock(@Param("item") String item, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Inventory i SET i.reserved = i.reserved - :qty WHERE i.item = :item")
    int releaseStock(@Param("item") String item, @Param("qty") int qty);
}

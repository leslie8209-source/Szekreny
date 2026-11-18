package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "lockers",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["personId"])],
)
data class LockerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val personId: Long,
    val lockerNumber: String,
)

@Entity(
    tableName = "phones",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["personId"])],
)
data class PhoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val personId: Long,
    val phoneNumber: String,
)

data class PersonWithDetails(
    @Embedded val person: PersonEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "personId",
    )
    val lockers: List<LockerEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "personId",
    )
    val phones: List<PhoneEntity>,
)

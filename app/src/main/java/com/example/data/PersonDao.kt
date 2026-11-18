package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Transaction
    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE ASC")
    fun observeAllPeopleWithDetails(): Flow<List<PersonWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLockers(lockers: List<LockerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhones(phones: List<PhoneEntity>)

    @Query("DELETE FROM lockers WHERE personId = :personId")
    suspend fun deleteLockersForPerson(personId: Long)

    @Query("DELETE FROM phones WHERE personId = :personId")
    suspend fun deletePhonesForPerson(personId: Long)

    @Query("DELETE FROM people WHERE id = :personId")
    suspend fun deletePersonById(personId: Long)

    @Transaction
    suspend fun savePersonWithDetails(
        personId: Long?,
        name: String,
        note: String,
        lockerNumbers: List<String>,
        phoneNumbers: List<String>,
        updatedAt: Long = System.currentTimeMillis(),
    ): Long {
        val targetId = if (personId == null || personId <= 0L) {
            insertPerson(
                PersonEntity(
                    name = name,
                    note = note,
                    updatedAt = updatedAt,
                ),
            )
        } else {
            updatePerson(
                PersonEntity(
                    id = personId,
                    name = name,
                    note = note,
                    updatedAt = updatedAt,
                ),
            )
            deleteLockersForPerson(personId)
            deletePhonesForPerson(personId)
            personId
        }

        if (lockerNumbers.isNotEmpty()) {
            insertLockers(
                lockerNumbers.map { number ->
                    LockerEntity(personId = targetId, lockerNumber = number)
                },
            )
        }
        if (phoneNumbers.isNotEmpty()) {
            insertPhones(
                phoneNumbers.map { phone ->
                    PhoneEntity(personId = targetId, phoneNumber = phone)
                },
            )
        }
        return targetId
    }
}

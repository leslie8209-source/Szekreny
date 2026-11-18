package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PersonRecord(
    val id: Long,
    val name: String,
    val note: String,
    val lockerNumbers: List<String>,
    val phoneNumbers: List<String>,
    val updatedAt: Long,
)

class PersonRepository(private val personDao: PersonDao) {
    val allPeople: Flow<List<PersonRecord>> = personDao.observeAllPeopleWithDetails()
        .map { rows ->
            rows.map { row ->
                PersonRecord(
                    id = row.person.id,
                    name = row.person.name,
                    note = row.person.note,
                    lockerNumbers = row.lockers.map { it.lockerNumber },
                    phoneNumbers = row.phones.map { it.phoneNumber },
                    updatedAt = row.person.updatedAt,
                )
            }
        }

    suspend fun savePerson(
        personId: Long?,
        name: String,
        note: String,
        lockerNumbers: List<String>,
        phoneNumbers: List<String>,
    ): Long {
        val cleanedName = name.trim()
        val cleanedNote = note.trim()
        val cleanedLockers = lockerNumbers
            .map { it.trim().removePrefix("#").trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
        val cleanedPhones = phoneNumbers
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.replace(Regex("[\\s\\-()]"), "") }

        return personDao.savePersonWithDetails(
            personId = personId,
            name = cleanedName,
            note = cleanedNote,
            lockerNumbers = cleanedLockers,
            phoneNumbers = cleanedPhones,
        )
    }

    suspend fun deletePerson(personId: Long) {
        personDao.deletePersonById(personId)
    }
}

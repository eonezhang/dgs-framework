/*
 * Copyright 2021 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.graphql.dgs.springdata

import graphql.language.ListType
import graphql.language.TypeName
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.core.CrudMethods
import org.springframework.data.repository.core.RepositoryMetadata
import org.springframework.data.util.TypeInformation
import java.lang.reflect.Method

internal class RepositoryDatafetcherManagerTest {

    data class Person(val name: String)
    interface PersonRepository : CrudRepository<Person, Int>

    @Test
    fun `Test creating query fields`() {
        val repositoryBeans = listOf(GraphqlRepositoryBeanDefinitionType(null, object: RepositoryMetadata{
            override fun getIdType(): Class<*> {
                return Int::class.java
            }

            override fun getDomainType(): Class<*> {
                return Person::class.java
            }

            override fun getRepositoryInterface(): Class<*> {
                return PersonRepository::class.java
            }

            override fun getReturnType(method: Method): TypeInformation<*> {
                throw NotImplementedError()
            }

            override fun getReturnedDomainClass(method: Method): Class<*> {
                throw NotImplementedError()
            }

            override fun getCrudMethods(): CrudMethods {
                throw NotImplementedError()
            }

            override fun isPagingRepository(): Boolean {
                throw NotImplementedError()
            }

            override fun getAlternativeDomainTypes(): MutableSet<Class<*>> {
                throw NotImplementedError()
            }

            override fun isReactiveRepository(): Boolean {
                return false
            }
        }))

        val repositoryDatafetcherManager = RepositoryDatafetcherManager(repositoryBeans)
        repositoryDatafetcherManager.createQueryFields()

        val type = repositoryDatafetcherManager.repositoryTypes().objectTypeExtensions()["Query"]
        assertThat(type).isNotNull
        assertThat(type?.get(0)?.fieldDefinitions).isNotEmpty
        val fieldDefinitions = type?.get(0)?.fieldDefinitions!!
        assertThat(fieldDefinitions).extracting("name").contains("allPersons", "person")

        val findAll = fieldDefinitions.find { it.name == "allPersons" }
        assertThat(findAll?.name).isEqualTo("allPersons")
        assertThat(findAll?.type.toString()).isEqualTo(ListType(TypeName("Person")).toString())

        val byId = fieldDefinitions.find { it.name == "person" }
        assertThat(byId?.name).isEqualTo("person")
        assertThat(byId?.type.toString()).isEqualTo(TypeName("Person").toString())
        assertThat(byId?.inputValueDefinitions!![0].name).isEqualTo("id")
    }
}
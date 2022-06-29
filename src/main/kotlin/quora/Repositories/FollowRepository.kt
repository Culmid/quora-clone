package quora.Repositories

import org.springframework.data.repository.CrudRepository
import quora.Entities.FollowRelationship

interface FollowRepository: CrudRepository<FollowRelationship, Int>
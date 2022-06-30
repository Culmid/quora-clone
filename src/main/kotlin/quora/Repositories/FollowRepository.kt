package quora.Repositories

import org.springframework.data.repository.CrudRepository
import quora.Entities.FollowRelationship
import quora.Entities.User

interface FollowRepository: CrudRepository<FollowRelationship, Int> {
    fun findByFollowedUserAndFollower(followedUser: User?, follower: User?): FollowRelationship?
}
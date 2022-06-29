package quora.Entities

import javax.persistence.*

@Entity
@Table(name="follows")
class FollowRelationship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followed_user_id")
    var followedUser: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id")
    var follower: User? = null
}
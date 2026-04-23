package com.back.together02be.achievement.repository;

import com.back.together02be.achievement.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    boolean existsByUsersIdAndAchievement_Code(Long usersId, String targetCode);
}

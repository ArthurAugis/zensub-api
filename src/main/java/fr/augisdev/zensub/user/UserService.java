package fr.augisdev.zensub.user;

import fr.augisdev.zensub.common.exception.ResourceNotFoundException;
import fr.augisdev.zensub.user.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public UserResponse getById(Long id) {
		return userRepository.findById(id)
				.map(UserResponse::from)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

}

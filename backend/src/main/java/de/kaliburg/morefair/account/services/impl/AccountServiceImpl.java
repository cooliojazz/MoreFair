package de.kaliburg.morefair.account.services.impl;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.kaliburg.morefair.account.model.AccountEntity;
import de.kaliburg.morefair.account.model.types.AccountAccessType;
import de.kaliburg.morefair.account.services.AccountService;
import de.kaliburg.morefair.account.services.repositories.AccountRepository;
import de.kaliburg.morefair.core.concurrency.CriticalRegion;
import de.kaliburg.morefair.security.UserDetailsWithUuid;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * This Service handles all the accounts.
 */
@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

  private static final Duration ACCOUNT_CACHE_EXPIRE_AFTER_ACCESS_DURATION =
      Duration.of(30, ChronoUnit.MINUTES);

  private final CriticalRegion semaphore = new CriticalRegion(1);
  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;

  private final LoadingCache<Long, AccountEntity> accountCache;
  private final LoadingCache<String, Long> usernameLookupCache;
  private final LoadingCache<UUID, Long> uuidLookupCache;
  private Long broadcasterAccountId;

  public AccountServiceImpl(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
    this.accountRepository = accountRepository;
    this.passwordEncoder = passwordEncoder;

    this.accountCache = Caffeine.newBuilder()
        .expireAfterAccess(ACCOUNT_CACHE_EXPIRE_AFTER_ACCESS_DURATION)
        .build(id -> this.accountRepository.findById(id).orElse(null));
    this.usernameLookupCache = Caffeine.newBuilder()
        .expireAfterAccess(ACCOUNT_CACHE_EXPIRE_AFTER_ACCESS_DURATION)
        .build(
            username -> this.accountRepository.findByUsername(username)
                .map(AccountEntity::getId)
                .orElse(null)
        );
    this.uuidLookupCache = Caffeine.newBuilder()
        .expireAfterAccess(ACCOUNT_CACHE_EXPIRE_AFTER_ACCESS_DURATION)
        .build(
            uuid -> this.accountRepository.findByUuid(uuid)
                .map(AccountEntity::getId)
                .orElse(null)
        );
  }


  @Override
  public Optional<AccountEntity> findByUuid(UUID uuid) {
    try (var ignored = semaphore.enter()) {
      return Optional.ofNullable(uuidLookupCache.get(uuid)).map(accountCache::get);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<AccountEntity> findById(Long id) {
    try (var ignored = semaphore.enter()) {
      return Optional.ofNullable(accountCache.get(id));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<AccountEntity> findByUsername(String username) {
    try (var ignored = semaphore.enter()) {
      return Optional.of(usernameLookupCache.get(username)).map(accountCache::get);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<AccountEntity> findBroadcaster() {
    try (var ignored = semaphore.enter()) {
      if (broadcasterAccountId == null) {
        List<AccountEntity> broadcasterAccounts =
            accountRepository.findByAccessRoleOrderByIdAsc(AccountAccessType.BROADCASTER);
        if (broadcasterAccounts.isEmpty()) {
          return Optional.empty();
        }

        this.broadcasterAccountId = broadcasterAccounts.get(0).getId();
      }

      return Optional.of(accountCache.get(this.broadcasterAccountId));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public Optional<AccountEntity> create(String username, String password, Integer ip,
      boolean isGuest) {
    AccountEntity result = new AccountEntity();
    result.setUsername(username);
    result.setPassword(passwordEncoder.encode(password));
    result.setLastIp(ip);
    result.setGuest(isGuest);

    try (var ignored = semaphore.enter()) {
      result = accountRepository.save(result);
      log.debug("Created Mystery Guest (#{})", result.getId());
      return Optional.of(result);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<AccountEntity> findByDisplayName(String name) {
    try (var ignored = semaphore.enter()) {
      // This creates 2 queries but only gets called by an admin-request, so rather rarely
      List<AccountEntity> accounts = accountRepository.findAccountsByDisplayNameIsContaining(
          name);
      accounts = accountCache.getAll(accounts.stream().map(AccountEntity::getId).toList())
          .values().stream().toList();
      return accounts;

    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<AccountEntity> searchByIp(Integer ip) {
    try (var ignored = semaphore.enter()) {
      List<Long> list = accountRepository.findTop100ByLastIpOrderByLastLoginDesc(ip).stream()
          .map(AccountEntity::getId)
          .toList();

      return accountCache.getAll(list).values().stream().toList();

    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public AccountEntity save(AccountEntity account) {
    try (var ignored = semaphore.enter()) {
      account = accountRepository.save(account);
      accountCache.put(account.getId(), account);
      return account;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<AccountEntity> saveAll(List<AccountEntity> accounts) {
    try (var ignored = semaphore.enter()) {
      accounts = accountRepository.saveAll(accounts);
      accountCache.putAll(
          accounts.stream().collect(Collectors.toMap(AccountEntity::getId, account -> account))
      );

      return accounts;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    try (var ignored = semaphore.enter()) {
      AccountEntity account = accountRepository.findByUsername(username)
          .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

      Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
      authorities.add(new SimpleGrantedAuthority(account.getAccessRole().name()));

      return new UserDetailsWithUuid(
          account.getUsername(),
          account.getPassword(),
          authorities,
          account.getUuid()
      );
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}

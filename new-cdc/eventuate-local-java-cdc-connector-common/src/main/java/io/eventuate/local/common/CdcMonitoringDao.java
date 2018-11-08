package io.eventuate.local.common;

import io.eventuate.javaclient.spring.jdbc.EventuateSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class CdcMonitoringDao {
  protected Logger logger = LoggerFactory.getLogger(getClass());

  private JdbcTemplate jdbcTemplate;
  private EventuateSchema eventuateSchema;
  private int monitoringRetryIntervalInMilliseconds;
  private int monitoringRetryAttempts;

  public CdcMonitoringDao(DataSource dataSource,
                          EventuateSchema eventuateSchema,
                          int monitoringRetryIntervalInMilliseconds,
                          int monitoringRetryAttempts) {
    jdbcTemplate = new JdbcTemplate(dataSource);
    this.eventuateSchema = eventuateSchema;
    this.monitoringRetryIntervalInMilliseconds = monitoringRetryIntervalInMilliseconds;
    this.monitoringRetryAttempts = monitoringRetryAttempts;
  }

  public void update(long readerId) {

    DaoUtils.handleConnectionLost(
            monitoringRetryAttempts,
            monitoringRetryIntervalInMilliseconds,
            () -> {
              int rows = jdbcTemplate.update(String.format("update %s set last_time = ? where reader_id = ?",
                      eventuateSchema.qualifyTable("cdc_monitoring")), System.currentTimeMillis(), readerId);

              if (rows == 0) {
                jdbcTemplate.update(String.format("insert into %s (reader_id, last_time) values (?, ?)",
                        eventuateSchema.qualifyTable("cdc_monitoring")), readerId, System.currentTimeMillis());
              }

              return null;
            },
            () -> {});
  }
}

-- glx_mails table
CREATE TABLE glx_mails(
  id SERIAL PRIMARY KEY,
  gateway INT NOT NULL,
  mail_name VARCHAR(60) NOT NULL,
  to_email VARCHAR(125) NOT NULL,
  from_email VARCHAR(125) NOT NULL,
  sent_at TIMESTAMP DEFAULT NULL
);
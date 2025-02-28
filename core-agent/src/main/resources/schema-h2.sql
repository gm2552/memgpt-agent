CREATE
    TABLE
    agents(
              id uuid NOT NULL PRIMARY KEY,
              version INTEGER NOT NULL,
              create_dt_time TIMESTAMP WITH TIME ZONE NOT NULL,
              update_dt_time TIMESTAMP WITH TIME ZONE NOT NULL,
              agent_name VARCHAR(100) NOT NULL,
              description VARCHAR(500) NOT NULL DEFAULT '',
              message_ids json NOT NULL,
              tool_ids json NOT NULL,
              system_prompt VARCHAR(50000) NOT NULL,
              context_window INTEGER NOT NULL,
              metadata_labels json
);

CREATE
    TABLE
    messages(
              id uuid NOT NULL PRIMARY KEY,
              agent_id uuid NOT NULL,
              role VARCHAR(20) NOT NULL,
              text VARCHAR,
              model VARCHAR(50),
              name VARCHAR(100),
              tool_calls json NOT NULL,
              tool_call_id VARCHAR,
              create_dt_time TIMESTAMP WITH TIME ZONE NOT NULL,
              update_dt_time TIMESTAMP WITH TIME ZONE NOT NULL,
              step_id VARCHAR(100)
);

CREATE
    TABLE
    blocks(
            id uuid NOT NULL PRIMARY KEY,
            version INTEGER NOT NULL,
            agent_id uuid NOT NULL,
            block_value VARCHAR NOT NULL,
            size_limit INTEGER NOT NULL,
            label VARCHAR(50) NOT NULL,
            metadata json,
            description VARCHAR(500) NOT NULL DEFAULT '',
            create_dt_time TIMESTAMP WITH TIME ZONE NOT NULL,
            update_dt_time TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE
    TABLE
    tools(
              id uuid NOT NULL PRIMARY KEY,
              name VARCHAR(100) NOT NULL,
              description VARCHAR(500) DEFAULT '',
              fq_class_name VARCHAR(256) NOT NULL,
              fq_input_class_name VARCHAR(256),
              return_character_limit INTEGER,
              create_dt_time TIMESTAMP WITH TIME ZONE NOT NULL,
              update_dt_time TIMESTAMP WITH TIME ZONE NOT NULL
);
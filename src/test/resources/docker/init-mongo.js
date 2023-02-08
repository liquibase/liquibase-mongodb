db.createUser(
    {
        user: "lbuser",
        pwd: "LiquibasePass1",
        roles: [
            {
                role: "readWrite",
                db: "lbcat"
            }
        ]
    }
);
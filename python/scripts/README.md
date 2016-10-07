# Get token for root user
> setenv VOTOKEN `curl "http://localhost:7001/login?username=root&password=D0n't-suck"`

# Initiate the VOSpace
> ./admintool init --token=$VOTOKEN --basedir=/Users/mjg/Projects/test/data

# Add a user
>./admintool adduser  --token=$VOTOKEN --basedir=/Users/mjg/Projects/test/data --vosroot='vos://nvo.caltech~vospace' vosurl='http://localhost:8080/vospace-2.0/vospace' --name='sarah'

# Validate the metadata in VOSpace from disk truth
>./admintool validate --start=vos://nvo.caltech~vospace

# Reinitialize the metadata in VOSpace from disk truth
>./admintool validate --start=vos://nvo.caltech~vospace --fix=True

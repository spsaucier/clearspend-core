function populate(jwt, user, registration) {
  if (user.data && user.data.businessId) {
    jwt.businessId = user.data.businessId;
  } else {
    jwt.businessId = "nope";
  }
  if (user.id) {
    jwt.userId = user.id;
  }
}
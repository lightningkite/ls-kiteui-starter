// Single-purpose: hamburger toggle for narrow viewports.
document.addEventListener("click", function (e) {
  var btn = e.target.closest("[data-nav-toggle]");
  if (!btn) return;
  var nav = document.querySelector("header.appbar nav");
  if (nav) nav.classList.toggle("open");
});

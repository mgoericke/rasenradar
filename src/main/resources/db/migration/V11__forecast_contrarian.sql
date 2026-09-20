-- Spec 05, "Gegen den Strom": whether the forecast's tendency diverged from the statistical
-- baseline's tendency at commit time. Null for forecasts committed before this existed --
-- the baseline used at that moment is not reconstructible, so no marker is shown for them.
alter table forecast add column contrarian boolean;

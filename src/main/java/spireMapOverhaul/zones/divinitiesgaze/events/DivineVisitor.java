package spireMapOverhaul.zones.divinitiesgaze.events;

import basemod.ReflectionHacks;
import basemod.abstracts.events.PhasedEvent;
import basemod.abstracts.events.phases.CombatPhase;
import basemod.abstracts.events.phases.TextPhase;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import spireMapOverhaul.SpireAnniversary6Mod;
import spireMapOverhaul.zones.divinitiesgaze.divinities.DivineBeing;
import spireMapOverhaul.zones.divinitiesgaze.divinities.DivineBeingManager;
import spireMapOverhaul.zones.divinitiesgaze.divinities.impl.*;

public class DivineVisitor extends PhasedEvent {
  public static final String ID = SpireAnniversary6Mod.makeID("DivineVisitor");
  private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString(ID);
  private static final String[] DESCRIPTIONS = eventStrings.DESCRIPTIONS;
  private static final String[] OPTIONS = eventStrings.OPTIONS;
  private static final String title = eventStrings.NAME;
  private static final String IMAGE = SpireAnniversary6Mod.makeEventPath("DivinitiesGaze/Appearance.png");

  private final DivineBeing divinity;

  public DivineVisitor() {
//    this(DivineBeingManager.getDivinityForEvent());
    this(new Jurors());
  }

  public DivineVisitor(DivineBeing divinity) {
    super(ID, String.format(title, ""), IMAGE);
    this.divinity = divinity;
    this.imageEventText.loadImage(IMAGE);

    registerPhase(Phase.ENTRANCE, new TextPhase(DESCRIPTIONS[0]).addOption(OPTIONS[0], i -> {
      this.imageEventText.loadImage(this.divinity.getDivinityStrings().getImagePath());
      ReflectionHacks.setPrivate(this, AbstractImageEvent.class, "title", String.format(title, " - " + this.divinity.getDivinityStrings().getName()));
      this.imageEventText.show(super.title, this.body);
      transitionKey(Phase.APPEARANCE);
    }));

    AbstractCard boonCard = CardLibrary.getCard(this.divinity.getDivinityStrings().getBoonCardId());
    String combatPhaseKey = this.divinity.getCombatPhaseKey();
    registerPhase(Phase.APPEARANCE, new TextPhase(this.divinity.getDivinityStrings().getEventText()) {
          @Override
          public void update() {
            super.update();
            if(combatPhaseKey.isEmpty() && DivineVisitor.this.divinity.doUpdate()) {
              transitionKey(Phase.ACCEPT);
            }
          }
        }
        .addOption(new TextPhase.OptionInfo(String.format(OPTIONS[1], boonCard.name), boonCard).setOptionResult(i -> {
          AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(boonCard, (float) Settings.WIDTH / 2.0F, (float)Settings.HEIGHT / 2.0F));
          transitionKey(Phase.ACCEPT);
        }))
        .addOption(new TextPhase.OptionInfo(this.divinity.isEventOptionEnabled().get()? this.divinity.getEventButtonText() : this.divinity.getEventButtonUnavailableText())
            .setOptionResult(this.divinity.doEventButtonAction().andThen(i -> transitionKey((!combatPhaseKey.isEmpty()) ? Phase.COMBAT : Phase.ACCEPT)))
            .enabledCondition(this.divinity.isEventOptionEnabled())
        )
        .addOption(OPTIONS[2], i -> {
          this.imageEventText.loadImage(IMAGE);
          ReflectionHacks.setPrivate(this, AbstractImageEvent.class, "title", String.format(title, ""));
          this.imageEventText.show(super.title, this.body);
          transitionKey(Phase.REJECT);
        })
    );

    registerPhase(Phase.ACCEPT, new TextPhase(this.divinity.getEventAcceptText() + DESCRIPTIONS[1]).addOption(OPTIONS[3], i -> {
      AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
      openMap();
    }));

    registerPhase(Phase.REJECT, new TextPhase(this.divinity.getEventRejectText() + DESCRIPTIONS[1]).addOption(OPTIONS[3], i -> {
      AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
      openMap();
    }));

    if(!combatPhaseKey.isEmpty()) {
      registerPhase(Phase.COMBAT, new CombatPhase(combatPhaseKey).addRewards(true, this.divinity.getCombatRewards()).setNextKey(Phase.ACCEPT));
    }

    transitionKey(Phase.ENTRANCE);
  }

  private enum Phase {
    ACCEPT, APPEARANCE, ENTRANCE, REJECT, COMBAT
  }
}

import type { CompletedStep } from '../types';

interface Props {
  completedSteps: CompletedStep[];
  currentStepIndex: number;
  totalSteps: number;
}

export default function StepChecklist({ completedSteps, currentStepIndex, totalSteps }: Props) {
  const completedIndexes = new Set(completedSteps.map((s) => s.stepIndex));

  return (
    <div className="checklist">
      {Array.from({ length: totalSteps }, (_, i) => {
        const completed = completedIndexes.has(i);
        const isCurrent = i === currentStepIndex && !completed;
        const step = completedSteps.find((s) => s.stepIndex === i);

        return (
          <div
            key={i}
            className={`checklist-item ${completed ? 'done' : ''} ${isCurrent ? 'current' : ''}`}
          >
            <span className="checklist-dot" />
            <span>
              {step ? step.stepName : `Step ${i + 1}`}
              {completed && step && (
                <span style={{ marginLeft: '6px', fontSize: '11px', color: 'var(--text-muted)' }}>
                  — {step.confirmedValue}
                </span>
              )}
            </span>
          </div>
        );
      })}
    </div>
  );
}

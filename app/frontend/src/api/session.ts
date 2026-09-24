import client from './client';
import type {
  Session,
  StepInfo,
  UploadResponse,
  UploadDocumentResponse,
  ManualValueResponse,
  ConfirmStepResponse,
  SubmitResponse,
  Document,
} from '../types';

export const uploadPdf = async (file: File): Promise<UploadResponse> => {
  const form = new FormData();
  form.append('file', file);
  const res = await client.post<UploadResponse>('/sessions/upload-730', form);
  return res.data;
};

export const getSessions = async (): Promise<Session[]> => {
  const res = await client.get<Session[]>('/sessions');
  return res.data;
};

export const getCurrentStep = async (sessionId: string): Promise<StepInfo> => {
  const res = await client.get<StepInfo>(`/sessions/${sessionId}/current-step`);
  return res.data;
};

export const uploadDocument = async (
  sessionId: string,
  file: File
): Promise<UploadDocumentResponse> => {
  const form = new FormData();
  form.append('file', file);
  const res = await client.post<UploadDocumentResponse>(
    `/sessions/${sessionId}/upload-document`,
    form
  );
  return res.data;
};

export const confirmStep = async (
  sessionId: string,
  confirmedValue: string
): Promise<ConfirmStepResponse> => {
  const res = await client.post<ConfirmStepResponse>(`/sessions/${sessionId}/confirm-step`, {
    confirmedValue,
  });
  return res.data;
};

export const submitSession = async (sessionId: string): Promise<SubmitResponse> => {
  const res = await client.post<SubmitResponse>(`/sessions/${sessionId}/submit`);
  return res.data;
};

export const submitManualValue = async (
  sessionId: string,
  userValue: string
): Promise<ManualValueResponse> => {
  const res = await client.post<ManualValueResponse>(
    `/sessions/${sessionId}/submit-manual-value`,
    { userValue }
  );
  return res.data;
};

export const getDocuments = async (): Promise<Document[]> => {
  const res = await client.get<Document[]>('/documents');
  return res.data;
};

/**
 * Restituisce l'URL per visualizzare il PDF originale della sessione.
 * Il token JWT viene aggiunto automaticamente dall'interceptor axios.
 */
export const getPdfUrl = (sessionId: string): string => {
  return `/api/sessions/${sessionId}/pdf`;
};
